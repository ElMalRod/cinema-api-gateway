package com.cinema.gateway.security;

import com.cinema.gateway.exception.GatewayAuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicKeyProviderTest {

    private final AtomicInteger calls = new AtomicInteger();
    private String publicKeyString;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();
        publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Test
    void shouldCallAuthServiceOnFirstRequest() {
        // Arrange
        PublicKeyProvider provider = buildProvider(successfulExchangeFunction());

        // Act
        Mono<RSAPublicKey> result = provider.getPublicKey();

        // Assert
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void shouldReturnCachedKeyOnSecondRequest() {
        // Arrange
        PublicKeyProvider provider = buildProvider(successfulExchangeFunction());

        // Act
        Mono<RSAPublicKey> first = provider.getPublicKey();
        Mono<RSAPublicKey> second = provider.getPublicKey();

        // Assert
        StepVerifier.create(first).expectNextCount(1).verifyComplete();
        StepVerifier.create(second).expectNextCount(1).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void shouldThrowGatewayAuthExceptionWhenAuthServiceFails() {
        // Arrange
        PublicKeyProvider provider = buildProvider(failingExchangeFunction());

        // Act
        Mono<RSAPublicKey> result = provider.getPublicKey();

        // Assert
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertEquals(GatewayAuthException.class, error.getClass());
                    assertEquals(HttpStatus.UNAUTHORIZED, ((GatewayAuthException) error).getStatus());
                })
                .verify();
    }

    private PublicKeyProvider buildProvider(ExchangeFunction exchangeFunction) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        return new PublicKeyProvider(builder, new ObjectMapper(), "http://auth-service:8081");
    }

    private ExchangeFunction successfulExchangeFunction() {
        return request -> {
            calls.incrementAndGet();
            return Mono.just(jsonResponse(Map.of("publicKey", publicKeyString)));
        };
    }

    private ExchangeFunction failingExchangeFunction() {
        return request -> {
            calls.incrementAndGet();
            return Mono.error(new RuntimeException("auth-service down"));
        };
    }

    private ClientResponse jsonResponse(Map<String, String> body) {
        try {
            String json = new ObjectMapper().writeValueAsString(body);
            return ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}

