package com.cinema.gateway.filter;

import com.cinema.gateway.constants.GatewayConstants;
import com.cinema.gateway.exception.GatewayAuthException;
import com.cinema.gateway.security.JwtClaimsExtractor;
import com.cinema.gateway.security.PublicKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifyJWTFilterFactoryTest {

    private PublicKeyProvider publicKeyProvider;
    private JwtClaimsExtractor claimsExtractor;
    private GatewayFilterChain chain;
    private GatewayFilter filter;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        publicKeyProvider = mock(PublicKeyProvider.class);
        claimsExtractor = mock(JwtClaimsExtractor.class);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        VerifyJWTFilterFactory factory = new VerifyJWTFilterFactory(publicKeyProvider, claimsExtractor);
        filter = factory.apply(new VerifyJWTFilterFactory.Config());
    }

    @Test
    void shouldFailWhenBearerTokenIsMissing() {
        MockServerWebExchange exchange = exchangeWithoutToken();

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();

        verify(chain, never()).filter(any());
    }

    @Test
    void shouldContinueAndInjectHeadersWhenTokenIsValid() throws Exception {
        String token = signedToken(keyPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.of(Instant.now().plusSeconds(120)));
        when(claimsExtractor.extractUserId(token)).thenReturn(Optional.of("user-123"));
        when(claimsExtractor.extractRole(token)).thenReturn(Optional.of("CLIENT"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        ServerWebExchange decorated = exchangeCaptor.getValue();
        assertEquals("user-123", decorated.getRequest().getHeaders().getFirst(GatewayConstants.USER_ID_HEADER));
        assertEquals("CLIENT", decorated.getRequest().getHeaders().getFirst(GatewayConstants.USER_ROLE_HEADER));
    }

    @Test
    void shouldFailWhenTokenIsExpired() throws Exception {
        String token = signedToken(keyPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.of(Instant.now().minusSeconds(5)));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();
    }

    @Test
    void shouldFailWhenExpirationClaimIsMissing() throws Exception {
        String token = signedToken(keyPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();
    }

    @Test
    void shouldRefreshKeyWhenCachedSignatureValidationFails() throws Exception {
        KeyPair refreshedPair = generateKeyPair();
        String token = signedToken(refreshedPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(publicKeyProvider.refreshPublicKey()).thenReturn(Mono.just((RSAPublicKey) refreshedPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.of(Instant.now().plusSeconds(120)));
        when(claimsExtractor.extractUserId(token)).thenReturn(Optional.of("user-123"));
        when(claimsExtractor.extractRole(token)).thenReturn(Optional.of("CLIENT"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(publicKeyProvider).refreshPublicKey();
        verify(chain).filter(any());
    }

    @Test
    void shouldFailWhenSignatureRemainsInvalidAfterRefresh() throws Exception {
        KeyPair otherPair = generateKeyPair();
        String token = signedToken(otherPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(publicKeyProvider.refreshPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.of(Instant.now().plusSeconds(120)));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();
    }

    @Test
    void shouldFailWhenRequiredClaimsAreMissing() throws Exception {
        String token = signedToken(keyPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.of(Instant.now().plusSeconds(120)));
        when(claimsExtractor.extractUserId(token)).thenReturn(Optional.empty());
        when(claimsExtractor.extractRole(token)).thenReturn(Optional.of("CLIENT"));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();
    }

    @Test
    void shouldFailWhenRoleIsNotAllowed() throws Exception {
        String token = signedToken(keyPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.just((RSAPublicKey) keyPair.getPublic()));
        when(claimsExtractor.extractExpiration(token)).thenReturn(Optional.of(Instant.now().plusSeconds(120)));
        when(claimsExtractor.extractUserId(token)).thenReturn(Optional.of("user-123"));
        when(claimsExtractor.extractRole(token)).thenReturn(Optional.of("NOT_ALLOWED"));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();
    }

    @Test
    void shouldMapUnexpectedErrorsToUnauthorized() throws Exception {
        String token = signedToken(keyPair, Map.of("sub", "user-123"));
        MockServerWebExchange exchange = exchangeWithToken(token);

        when(publicKeyProvider.getPublicKey()).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    GatewayAuthException exception = assertInstanceOf(GatewayAuthException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
                })
                .verify();
    }

    private MockServerWebExchange exchangeWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/profile").build();
        return MockServerWebExchange.from(request);
    }

    private MockServerWebExchange exchangeWithToken(String token) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/profile")
                .header(GatewayConstants.AUTHORIZATION_HEADER, GatewayConstants.BEARER_PREFIX + token)
                .build();
        return MockServerWebExchange.from(request);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String signedToken(KeyPair pair, Map<String, Object> payloadClaims) throws Exception {
        String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payloadClaims));
        String content = header + "." + payload;

        Signature signature = Signature.getInstance(GatewayConstants.SIGNATURE_ALGORITHM);
        signature.initSign(pair.getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        String signed = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        return content + "." + signed;
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
