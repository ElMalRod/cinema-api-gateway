package com.cinema.gateway.filter;

import com.cinema.gateway.constants.GatewayConstants;
import com.cinema.gateway.exception.GatewayAuthException;
import com.cinema.gateway.security.JwtClaimsExtractor;
import com.cinema.gateway.security.PublicKeyProvider;
import com.cinema.gateway.security.PublicRouteValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final PublicRouteValidator publicRouteValidator;
    private final PublicKeyProvider publicKeyProvider;
    private final JwtClaimsExtractor claimsExtractor;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (publicRouteValidator.isPublicRoute(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        return extractBearerToken(exchange.getRequest())
                .map(token -> validateTokenAndContinue(exchange, chain, token))
                .orElseGet(() -> writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing bearer token"));
    }

    private Mono<Void> validateTokenAndContinue(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        return publicKeyProvider.getPublicKey()
                .flatMap(key -> validateAndInject(exchange, chain, token, key))
                .onErrorResume(GatewayAuthException.class, ex -> writeError(exchange, ex.getStatus(), ex.getMessage()));
    }

    private Mono<Void> validateAndInject(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String token,
            RSAPublicKey publicKey
    ) {
        if (!isSignatureValid(token, publicKey) || isExpired(token)) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        Optional<String> userId = claimsExtractor.extractUserId(token);
        Optional<String> role = claimsExtractor.extractRole(token);
        if (userId.isEmpty() || role.isEmpty()) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "Required claims not found");
        }
        if (!GatewayConstants.ALLOWED_ROLES.contains(role.get())) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "Invalid role");
        }
        ServerWebExchange decorated = injectUserHeaders(exchange, userId.get(), role.get());
        return chain.filter(decorated);
    }

    private Optional<String> extractBearerToken(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION_HEADER))
                .filter(value -> value.startsWith(GatewayConstants.BEARER_PREFIX))
                .map(value -> value.substring(GatewayConstants.BEARER_PREFIX.length()));
    }

    private boolean isExpired(String token) {
        return claimsExtractor.extractExpiration(token)
                .map(expiration -> expiration.isBefore(Instant.now()))
                .orElse(true);
    }

    private boolean isSignatureValid(String token, RSAPublicKey publicKey) {
        String[] sections = token.split("\\.");
        if (sections.length != 3) {
            return false;
        }
        try {
            String signedData = sections[0] + "." + sections[1];
            byte[] signatureBytes = Base64.getUrlDecoder().decode(sections[2]);
            Signature signature = Signature.getInstance(GatewayConstants.SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(signedData.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (Exception exception) {
            log.warn("JWT signature validation failed: {}", exception.getMessage());
            return false;
        }
    }

    private ServerWebExchange injectUserHeaders(ServerWebExchange exchange, String userId, String role) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(GatewayConstants.USER_ID_HEADER, userId)
                .header(GatewayConstants.USER_ROLE_HEADER, role)
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = buildBody(status, message, exchange.getRequest().getPath().value());
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private byte[] buildBody(HttpStatus status, String message, String path) {
        try {
            return objectMapper.writeValueAsBytes(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", status.value(),
                    "message", message,
                    "path", path
            ));
        } catch (Exception exception) {
            return "{\"status\":500,\"message\":\"Error writing response\",\"path\":\"/\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
