package com.cinema.gateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayAuthExceptionTest {

    @Test
    void shouldExposeStatusAndMessage() {
        GatewayAuthException exception = new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid token");

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid token", exception.getMessage());
    }
}
