package com.cinema.gateway.security;

import com.cinema.gateway.constants.GatewayConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtClaimsExtractor {

    private final ObjectMapper objectMapper;

    public JwtClaimsExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> extractUserId(String token) {
        return extractClaims(token).flatMap(claims -> extractStringClaim(claims, GatewayConstants.CLAIM_USER_ID));
    }

    public Optional<String> extractRole(String token) {
        return extractClaims(token).flatMap(claims -> extractStringClaim(claims, GatewayConstants.CLAIM_ROLE));
    }

    public Optional<Instant> extractExpiration(String token) {
        return extractClaims(token).flatMap(this::extractExpirationFromClaims);
    }

    public Optional<Map<String, Object>> extractClaims(String token) {
        return decodePayload(token).flatMap(this::parseClaims);
    }

    private Optional<String> decodePayload(String token) {
        String[] sections = token.split("\\.");
        if (sections.length != 3) {
            return Optional.empty();
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(sections[1]);
            return Optional.of(new String(decoded));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> parseClaims(String payload) {
        try {
            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });
            return Optional.of(claims);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<String> extractStringClaim(Map<String, Object> claims, String claimName) {
        return Optional.ofNullable(claims.get(claimName))
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    private Optional<Instant> extractExpirationFromClaims(Map<String, Object> claims) {
        Object exp = claims.get(GatewayConstants.CLAIM_EXP);
        if (exp instanceof Number number) {
            return Optional.of(Instant.ofEpochSecond(number.longValue()));
        }
        if (exp instanceof String value && value.matches("\\d+")) {
            return Optional.of(Instant.ofEpochSecond(Long.parseLong(value)));
        }
        return Optional.empty();
    }
}
