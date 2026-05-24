package com.cinema.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldReturnTrueForForgotPasswordAndResetPasswordRoutes() {
        // Arrange
        MockServerHttpRequest forgot = MockServerHttpRequest.method(HttpMethod.POST, "/auth/forgot-password").build();
        MockServerHttpRequest reset = MockServerHttpRequest.method(HttpMethod.POST, "/auth/reset-password").build();

        // Act
        boolean forgotResult = validator.isPublicRoute(forgot);
        boolean resetResult = validator.isPublicRoute(reset);

        // Assert
        assertTrue(forgotResult);
        assertTrue(resetResult);
    }

    @Test
    void shouldReturnFalseForMoviesNestedPath() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/123/reviews").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForMovieByIdWhenMethodIsNotGet() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/movies/123").build();

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenMethodIsNull() {
        // Arrange
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getMethod()).thenReturn(null);
        when(request.getPath()).thenReturn(MockServerHttpRequest.get("/movies").build().getPath());

        // Act
        boolean result = validator.isPublicRoute(request);

        // Assert
        assertFalse(result);
    }

    // ── PublicMoviesApiStrategy ───────────────────────────────────────────────

    @Test
    void shouldReturnTrueForGetMoviesV1List() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/v1/movies").build();
        assertTrue(validator.isPublicRoute(request));
    }

    @Test
    void shouldReturnTrueForGetMoviesV1Detail() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/v1/movies/abc-123").build();
        assertTrue(validator.isPublicRoute(request));
    }

    @Test
    void shouldReturnTrueForGetMoviesV1NestedPath() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/v1/movies/abc-123/comments").build();
        assertTrue(validator.isPublicRoute(request));
    }

    @Test
    void shouldReturnTrueForGetMoviesV1Categories() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/v1/categories").build();
        assertTrue(validator.isPublicRoute(request));
    }

    @Test
    void shouldReturnTrueForGetMoviesV1CountriesClassifications() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, "/movies/v1/countries/00000001/classifications").build();
        assertTrue(validator.isPublicRoute(request));
    }

    @Test
    void shouldReturnFalseForPostMoviesV1List() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/movies/v1/movies").build();
        assertFalse(validator.isPublicRoute(request));
    }

    @Test
    void shouldReturnFalseForDeleteMoviesV1Movie() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.DELETE, "/movies/v1/movies/abc-123").build();
        assertFalse(validator.isPublicRoute(request));
    }
}
