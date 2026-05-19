package com.cinema.gateway.filter;

import com.cinema.gateway.constants.GatewayConstants;
import com.cinema.gateway.exception.GatewayAuthException;
import com.cinema.gateway.security.JwtClaimsExtractor;
import com.cinema.gateway.security.PublicKeyProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Component
public class VerifyJWTFilterFactory extends AbstractGatewayFilterFactory<VerifyJWTFilterFactory.Config> {

    private final PublicKeyProvider publicKeyProvider;
    private final JwtClaimsExtractor claimsExtractor;

    public VerifyJWTFilterFactory(PublicKeyProvider publicKeyProvider, JwtClaimsExtractor claimsExtractor) {
        super(Config.class);
        this.publicKeyProvider = publicKeyProvider;
        this.claimsExtractor = claimsExtractor;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> extractBearerToken(exchange.getRequest())
                .map(token -> validateTokenAndContinue(exchange, chain, token))
                .orElseGet(() -> Mono.error(new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Missing bearer token")));
    }

    private Mono<Void> validateTokenAndContinue(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain, String token) {
        return publicKeyProvider.getPublicKey()
                .flatMap(key -> validateAndInject(exchange, chain, token, key))
                .onErrorMap(GatewayAuthException.class, exception -> exception)
                .onErrorMap(exception -> new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
    }

    private Mono<Void> validateAndInject(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
            String token,
            RSAPublicKey publicKey
    ) {
        if (!isSignatureValid(token, publicKey) || isExpired(token)) {
            return Mono.error(new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        }
        Optional<String> userId = claimsExtractor.extractUserId(token);
        Optional<String> role = claimsExtractor.extractRole(token);
        if (userId.isEmpty() || role.isEmpty()) {
            return Mono.error(new GatewayAuthException(HttpStatus.FORBIDDEN, "Required claims not found"));
        }
        if (!GatewayConstants.ALLOWED_ROLES.contains(role.get())) {
            return Mono.error(new GatewayAuthException(HttpStatus.FORBIDDEN, "Invalid role"));
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

    public static class Config {
    }
}
