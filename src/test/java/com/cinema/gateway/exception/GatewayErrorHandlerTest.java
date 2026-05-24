package com.cinema.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GatewayErrorHandlerTest {

    @Test
    void shouldReturnErrorWhenResponseIsAlreadyCommitted() {
        GatewayErrorHandler handler = new GatewayErrorHandler(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users").build());
        exchange.getResponse().setComplete().block();
        RuntimeException expected = new RuntimeException("already committed");

        StepVerifier.create(handler.handle(exchange, expected))
                .expectErrorSatisfies(error -> assertEquals(expected, error))
                .verify();
    }

    @Test
    void shouldRenderGatewayAuthExceptionAsJson() {
        GatewayErrorHandler handler = new GatewayErrorHandler(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/reports").build());

        StepVerifier.create(handler.handle(exchange, new GatewayAuthException(HttpStatus.FORBIDDEN, "Denied")))
                .verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldResolveStatusFromResponseStatusException() {
        GatewayErrorHandler handler = new GatewayErrorHandler(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/movies/404").build());

        StepVerifier.create(handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")))
                .verifyComplete();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldUseDefaultMessageWhenExceptionMessageIsNull() {
        GatewayErrorHandler handler = new GatewayErrorHandler(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/movies").build());

        StepVerifier.create(handler.handle(exchange, new RuntimeException((String) null)))
                .verifyComplete();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldUseFallbackBodyWhenSerializationFails() throws Exception {
        ObjectMapper mapper = Mockito.mock(ObjectMapper.class);
        when(mapper.writeValueAsBytes(any())).thenThrow(new RuntimeException("serialization failure"));
        GatewayErrorHandler handler = new GatewayErrorHandler(mapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users").build());

        StepVerifier.create(handler.handle(exchange, new RuntimeException("boom")))
                .verifyComplete();

        String body = exchange.getResponse().getBodyAsString().block();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        assertEquals("application/json", exchange.getResponse().getHeaders().getContentType().toString());
        assertNotNull(body);
    }
}
