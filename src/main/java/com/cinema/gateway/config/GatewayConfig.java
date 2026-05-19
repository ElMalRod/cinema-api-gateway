package com.cinema.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(
            RouteLocatorBuilder builder,
            @Value("${gateway.services.auth}") String authService,
            @Value("${gateway.services.users}") String usersService,
            @Value("${gateway.services.movies}") String moviesService,
            @Value("${gateway.services.cinemas}") String cinemasService,
            @Value("${gateway.services.tickets}") String ticketsService,
            @Value("${gateway.services.ads}") String adsService,
            @Value("${gateway.services.reports}") String reportsService
    ) {
        return builder.routes()
                .route("auth-service", route -> route.path("/auth/**").uri(authService))
                .route("users-service", route -> route.path("/users/**").uri(usersService))
                .route("movies-service", route -> route.path("/movies/**").uri(moviesService))
                .route("cinemas-service", route -> route.path("/cinemas/**").uri(cinemasService))
                .route("tickets-service", route -> route.path("/tickets/**").uri(ticketsService))
                .route("ads-service", route -> route.path("/ads/**").uri(adsService))
                .route("reports-service", route -> route.path("/reports/**").uri(reportsService))
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
