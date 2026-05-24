package com.cinema.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicRouteValidator {

    private final List<RouteAccessStrategy> publicStrategies = List.of(
            new ExactRouteStrategy(HttpMethod.POST, "/auth/login"),
            new ExactRouteStrategy(HttpMethod.POST, "/auth/register"),
            new ExactRouteStrategy(HttpMethod.POST, "/auth/forgot-password"),
            new ExactRouteStrategy(HttpMethod.POST, "/auth/reset-password"),
            new ExactRouteStrategy(HttpMethod.GET, "/movies"),
            new ExactRouteStrategy(HttpMethod.GET, "/ads/active"),
            new MoviesByIdStrategy(),
            new PublicMoviesApiStrategy()
    );

    public boolean isPublicRoute(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        String path = request.getPath().value();
        return method != null && publicStrategies.stream().anyMatch(strategy -> strategy.matches(method, path));
    }

    private interface RouteAccessStrategy {
        boolean matches(HttpMethod method, String path);
    }

    private record ExactRouteStrategy(HttpMethod method, String path) implements RouteAccessStrategy {
        @Override
        public boolean matches(HttpMethod method, String path) {
            return this.method == method && this.path.equals(path);
        }
    }

    private static final class MoviesByIdStrategy implements RouteAccessStrategy {
        @Override
        public boolean matches(HttpMethod method, String path) {
            return method == HttpMethod.GET && path.matches("^/movies/[^/]+$");
        }
    }

    private static final class PublicMoviesApiStrategy implements RouteAccessStrategy {
        @Override
        public boolean matches(HttpMethod method, String path) {
            if (method != HttpMethod.GET) return false;
            return path.equals("/movies/v1/movies")
                    || path.startsWith("/movies/v1/movies/")
                    || path.equals("/movies/v1/categories")
                    || path.startsWith("/movies/v1/countries/");
        }
    }
}
