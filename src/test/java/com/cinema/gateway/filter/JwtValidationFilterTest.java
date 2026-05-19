package com.cinema.gateway.filter;

import com.cinema.gateway.constants.GatewayConstants;
import com.cinema.gateway.security.JwtClaimsExtractor;
import com.cinema.gateway.security.PublicKeyProvider;
import com.cinema.gateway.security.PublicRouteValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtValidationFilterTest {

    private JwtAuthFilter filter;
    private PublicRouteValidator publicRouteValidator;
    private PublicKeyProvider publicKeyProvider;
    private GatewayFilterChain chain;
    private KeyPair keyPair;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        publicRouteValidator = mock(PublicRouteValidator.class);
        publicKeyProvider = mock(PublicKeyProvider.class);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        filter = new JwtAuthFilter(publicRouteValidator, publicKeyProvider, new JwtClaimsExtractor(mapper), mapper);
    }

    @Test
    void shouldContinueFilterChainWhenTokenIsValid() throws Exception {
        // Arrange
        String token = buildLegacyToken(UUID.randomUUID().toString(), "CLIENT", Instant.now().plusSeconds(600).getEpochSecond(), keyPair);
        MockServerWebExchange exchange = buildProtectedExchange(token);
        when(publicRouteValidator.isPublicRoute(exchange.getRequest())).thenReturn(false);
        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldContinueFilterChainWhenTokenUsesSubAndRoles() throws Exception {
        // Arrange
        String token = buildModernToken(UUID.randomUUID().toString(), "CINEMA_ADMIN", Instant.now().plusSeconds(600).getEpochSecond(), keyPair);
        MockServerWebExchange exchange = buildProtectedExchange(token);
        when(publicRouteValidator.isPublicRoute(exchange.getRequest())).thenReturn(false);
        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
        // Arrange
        String token = buildLegacyToken(UUID.randomUUID().toString(), "CLIENT", Instant.now().minusSeconds(60).getEpochSecond(), keyPair);
        MockServerWebExchange exchange = buildProtectedExchange(token);
        when(publicRouteValidator.isPublicRoute(exchange.getRequest())).thenReturn(false);
        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenSignatureIsInvalid() throws Exception {
        // Arrange
        KeyPairGenerator otherGenerator = KeyPairGenerator.getInstance("RSA");
        otherGenerator.initialize(2048);
        KeyPair otherPair = otherGenerator.generateKeyPair();
        String token = buildLegacyToken(UUID.randomUUID().toString(), "CLIENT", Instant.now().plusSeconds(600).getEpochSecond(), otherPair);
        MockServerWebExchange exchange = buildProtectedExchange(token);
        when(publicRouteValidator.isPublicRoute(exchange.getRequest())).thenReturn(false);
        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldReturnForbiddenWhenRoleClaimIsMissing() throws Exception {
        // Arrange
        String token = buildTokenWithoutRole(UUID.randomUUID().toString(), Instant.now().plusSeconds(600).getEpochSecond());
        MockServerWebExchange exchange = buildProtectedExchange(token);
        when(publicRouteValidator.isPublicRoute(exchange.getRequest())).thenReturn(false);
        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldAllowPublicRouteWithoutToken() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(publicRouteValidator.isPublicRoute(exchange.getRequest())).thenReturn(true);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    private MockServerWebExchange buildProtectedExchange(String token) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/tickets")
                .header(HttpHeaders.AUTHORIZATION, GatewayConstants.BEARER_PREFIX + token)
                .build();
        return MockServerWebExchange.from(request);
    }

    private String buildLegacyToken(String userId, String role, long exp, KeyPair signingPair) throws Exception {
        return buildToken(Map.of("user_id", userId, "role", role, "exp", exp), signingPair);
    }

    private String buildModernToken(String userId, String role, long exp, KeyPair signingPair) throws Exception {
        return buildToken(Map.of("sub", userId, "roles", java.util.List.of(role), "email", "admin@test.com", "exp", exp), signingPair);
    }

    private String buildTokenWithoutRole(String userId, long exp) throws Exception {
        return buildToken(Map.of("sub", userId, "exp", exp), keyPair);
    }

    private String buildToken(Map<String, Object> payloadClaims, KeyPair signingPair) throws Exception {
        String header = encode(Map.of("alg", "RS256", "typ", "JWT"));
        String payload = encode(payloadClaims);
        String signature = sign(header + "." + payload, signingPair);
        return header + "." + payload + "." + signature;
    }

    private String encode(Map<String, Object> map) throws Exception {
        String json = mapper.writeValueAsString(map);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value, KeyPair pair) throws Exception {
        Signature signature = Signature.getInstance(GatewayConstants.SIGNATURE_ALGORITHM);
        signature.initSign(pair.getPrivate());
        signature.update(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }
}
