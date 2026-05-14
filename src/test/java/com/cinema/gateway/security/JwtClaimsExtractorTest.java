package com.cinema.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtClaimsExtractorTest {

    private JwtClaimsExtractor extractor;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        extractor = new JwtClaimsExtractor(mapper);
    }

    @Test
    void shouldExtractUserIdFromToken() throws Exception {
        // Arrange
        String expectedUserId = UUID.randomUUID().toString();
        String token = generateToken(expectedUserId, "CLIENT", Instant.now().plusSeconds(600).getEpochSecond());

        // Act
        Optional<String> userId = extractor.extractUserId(token);

        // Assert
        assertTrue(userId.isPresent());
        assertEquals(expectedUserId, userId.get());
    }

    @Test
    void shouldExtractRoleFromToken() throws Exception {
        // Arrange
        String token = generateToken(UUID.randomUUID().toString(), "SYSTEM_ADMIN", Instant.now().plusSeconds(600).getEpochSecond());

        // Act
        Optional<String> role = extractor.extractRole(token);

        // Assert
        assertTrue(role.isPresent());
        assertEquals("SYSTEM_ADMIN", role.get());
    }

    @Test
    void shouldReturnEmptyWhenClaimsAreMissing() throws Exception {
        // Arrange
        String token = generateTokenWithoutClaims();

        // Act
        Optional<String> userId = extractor.extractUserId(token);
        Optional<String> role = extractor.extractRole(token);

        // Assert
        assertTrue(userId.isEmpty());
        assertTrue(role.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenTokenIsMalformed() {
        // Arrange
        String malformedToken = "this.is.not-valid-base64";

        // Act
        Optional<String> userId = extractor.extractUserId(malformedToken);
        Optional<String> role = extractor.extractRole(malformedToken);

        // Assert
        assertTrue(userId.isEmpty());
        assertTrue(role.isEmpty());
    }

    private String generateToken(String userId, String role, long exp) throws Exception {
        KeyPair keyPair = buildKeyPair();
        String header = encodeJson(Map.of("alg", "RS256", "typ", "JWT"));
        String payload = encodeJson(Map.of("user_id", userId, "role", role, "exp", exp));
        String signature = sign(header + "." + payload, keyPair);
        return header + "." + payload + "." + signature;
    }

    private String generateTokenWithoutClaims() throws Exception {
        KeyPair keyPair = buildKeyPair();
        String header = encodeJson(Map.of("alg", "RS256", "typ", "JWT"));
        String payload = encodeJson(Map.of("exp", Instant.now().plusSeconds(600).getEpochSecond()));
        String signature = sign(header + "." + payload, keyPair);
        return header + "." + payload + "." + signature;
    }

    private KeyPair buildKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        String json = mapper.writeValueAsString(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String content, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }
}
