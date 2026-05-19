package com.cinema.gateway.config;

import com.cinema.gateway.filter.VerifyJWTFilterFactory;
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
            VerifyJWTFilterFactory verifyJWTFilterFactory,
            @Value("${gateway.services.auth}") String authService,
            @Value("${gateway.services.users}") String usersService,
            @Value("${gateway.services.movies}") String moviesService,
            @Value("${gateway.services.cinemas}") String cinemasService,
            @Value("${gateway.services.tickets}") String ticketsService,
            @Value("${gateway.services.ads}") String adsService,
            @Value("${gateway.services.reports}") String reportsService
    ) {
        VerifyJWTFilterFactory.Config verifyConfig = new VerifyJWTFilterFactory.Config();

        return builder.routes()
                .route("auth-public-routes", route -> route.path("/auth/**").uri(authService))
                .route("movies-public-routes", route -> route.path("/movies", "/movies/*").uri(moviesService))
                .route("users-private-routes", route -> route.path("/users/**")
                        .filters(filter -> filter.filter(verifyJWTFilterFactory.apply(verifyConfig)))
                        .uri(usersService))
                .route("movies-private-routes", route -> route.path("/movies/**")
                        .filters(filter -> filter.filter(verifyJWTFilterFactory.apply(verifyConfig)))
                        .uri(moviesService))
                .route("cinemas-private-routes", route -> route.path("/cinemas/**")
                        .filters(filter -> filter.filter(verifyJWTFilterFactory.apply(verifyConfig)))
                        .uri(cinemasService))
                .route("tickets-private-routes", route -> route.path("/tickets/**")
                        .filters(filter -> filter.filter(verifyJWTFilterFactory.apply(verifyConfig)))
                        .uri(ticketsService))
                .route("ads-private-routes", route -> route.path("/ads/**")
                        .filters(filter -> filter.filter(verifyJWTFilterFactory.apply(verifyConfig)))
                        .uri(adsService))
                .route("reports-private-routes", route -> route.path("/reports/**")
                        .filters(filter -> filter.filter(verifyJWTFilterFactory.apply(verifyConfig)))
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
