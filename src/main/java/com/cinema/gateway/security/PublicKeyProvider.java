package com.cinema.gateway.security;

import com.cinema.gateway.constants.GatewayConstants;
import com.cinema.gateway.exception.GatewayAuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class PublicKeyProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicReference<RSAPublicKey> cachedKey = new AtomicReference<>();
    private final AtomicReference<Mono<RSAPublicKey>> inFlight = new AtomicReference<>();

    public PublicKeyProvider(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gateway.auth-service.base-url}") String authServiceBaseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(authServiceBaseUrl).build();
        this.objectMapper = objectMapper;
    }

    public Mono<RSAPublicKey> getPublicKey() {
        RSAPublicKey cached = cachedKey.get();
        if (cached != null) {
            return Mono.just(cached);
        }
        Mono<RSAPublicKey> current = inFlight.get();
        if (current != null) {
            return current;
        }
        Mono<RSAPublicKey> created = fetchAndCache().cache();
        if (!inFlight.compareAndSet(null, created)) {
            return inFlight.get();
        }
        return created.doFinally(signal -> inFlight.set(null));
    }

    private Mono<RSAPublicKey> fetchAndCache() {
        return webClient.get()
                .uri(GatewayConstants.PUBLIC_KEY_ENDPOINT)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::resolveRawKey)
                .map(this::parsePublicKey)
                .doOnNext(cachedKey::set)
                .doOnNext(key -> log.info("Public key cached successfully"))
                .onErrorMap(ex -> new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Unable to load public key"));
    }

    private String resolveRawKey(String body) {
        String normalized = body == null ? "" : body.trim();
        if (normalized.startsWith("-----BEGIN PUBLIC KEY-----")) {
            return normalized;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(normalized);
            return resolveFromJson(jsonNode);
        } catch (Exception exception) {
            throw new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid public key response format");
        }
    }

    private String resolveFromJson(JsonNode body) {
        if (body.has("publicKey")) {
            return body.get("publicKey").asText();
        }
        if (body.has("public_key")) {
            return body.get("public_key").asText();
        }
        throw new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid public key response format");
    }

    private RSAPublicKey parsePublicKey(String rawKey) {
        try {
            String normalized = normalizeKey(rawKey);
            byte[] decoded = Base64.getDecoder().decode(normalized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception exception) {
            throw new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid RS256 public key");
        }
    }

    private String normalizeKey(String rawKey) {
        return rawKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
