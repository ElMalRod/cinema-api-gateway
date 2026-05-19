package com.cinema.gateway.security;

import com.cinema.gateway.constants.GatewayConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtClaimsExtractor {

    private final ObjectMapper objectMapper;

    public JwtClaimsExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> extractUserId(String token) {
        return extractClaims(token).flatMap(this::extractUserIdClaim);
    }

    public Optional<String> extractRole(String token) {
        return extractClaims(token).flatMap(this::extractRoleClaim);
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
            return Optional.of(new String(decoded, StandardCharsets.UTF_8));
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

    private Optional<String> extractUserIdClaim(Map<String, Object> claims) {
        Optional<String> userId = extractStringClaim(claims, GatewayConstants.CLAIM_USER_ID);
        return userId.isPresent() ? userId : extractStringClaim(claims, GatewayConstants.CLAIM_SUB);
    }

    private Optional<String> extractRoleClaim(Map<String, Object> claims) {
        Optional<String> role = extractStringClaim(claims, GatewayConstants.CLAIM_ROLE);
        if (role.isPresent()) {
            return role;
        }
        Object rolesValue = claims.get(GatewayConstants.CLAIM_ROLES);
        if (!(rolesValue instanceof List<?> roles) || roles.isEmpty()) {
            return Optional.empty();
        }
        Object firstRole = roles.get(0);
        return firstRole instanceof String first ? Optional.of(first) : Optional.empty();
    }

    private Optional<String> extractStringClaim(Map<String, Object> claims, String claimName) {
        return Optional.ofNullable(claims.get(claimName))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(value -> !value.isBlank());
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
