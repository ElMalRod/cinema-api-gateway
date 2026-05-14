package com.cinema.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class GatewayConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() == null
                    ? "unknown"
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }

    @Bean
    public RouteLocator routeLocator(
            RouteLocatorBuilder builder,
            KeyResolver ipKeyResolver,
            @Value("${gateway.services.auth}") String authService,
            @Value("${gateway.services.users}") String usersService,
            @Value("${gateway.services.movies}") String moviesService,
            @Value("${gateway.services.cinemas}") String cinemasService,
            @Value("${gateway.services.tickets}") String ticketsService,
            @Value("${gateway.services.ads}") String adsService,
            @Value("${gateway.services.reports}") String reportsService
    ) {
        return builder.routes()
                .route("auth-service", route -> route.path("/auth/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(authService))
                .route("users-service", route -> route.path("/users/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(usersService))
                .route("movies-service", route -> route.path("/movies/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(moviesService))
                .route("cinemas-service", route -> route.path("/cinemas/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(cinemasService))
                .route("tickets-service", route -> route.path("/tickets/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(ticketsService))
                .route("ads-service", route -> route.path("/ads/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(adsService))
                .route("reports-service", route -> route.path("/reports/**")
                        .filters(filter -> filter.requestRateLimiter(config -> config.setKeyResolver(ipKeyResolver)))
                        .uri(reportsService))
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
