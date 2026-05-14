package com.cinema.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicRouteValidatorTest {

    private PublicRouteValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PublicRouteValidator();
    }

    @Test
    void shouldReturnTrueForLoginRoute() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/auth/login").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForRegisterRoute() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/auth/register").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForMoviesRoute() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForMovieByIdRoute() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/123").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForTicketsRoute() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/tickets").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForUsersProfileRoute() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/users/profile").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertFalse(result);
    }
}
