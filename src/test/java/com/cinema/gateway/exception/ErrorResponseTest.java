package com.cinema.gateway.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErrorResponseTest {

    @Test
    void shouldBuildUnauthorizedResponse() {
        // Arrange
        Instant now = Instant.now();

        // Act
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(now)
                .status(401)
                .message("Unauthorized")
                .path("/users/profile")
                .build();

        // Assert
        assertNotNull(response.timestamp());
        assertEquals(401, response.status());
        assertEquals("Unauthorized", response.message());
    }

    @Test
    void shouldBuildForbiddenResponse() {
        // Arrange
        Instant now = Instant.now();

        // Act
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(now)
                .status(403)
                .message("Forbidden")
                .path("/reports/admin")
                .build();

        // Assert
        assertNotNull(response.timestamp());
        assertEquals(403, response.status());
        assertEquals("Forbidden", response.message());
    }

    @Test
    void shouldFailWhenAnyFieldIsNull() {
        // Arrange
        ErrorResponse.Builder builder = ErrorResponse.builder();

        // Act + Assert
        assertThrows(NullPointerException.class, () -> builder
                .timestamp(null)
                .status(401)
                .message("Unauthorized")
                .path("/test")
                .build());
    }
}
