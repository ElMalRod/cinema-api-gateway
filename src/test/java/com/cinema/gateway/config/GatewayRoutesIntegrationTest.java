package com.cinema.gateway.config;

import com.cinema.gateway.CinemaGatewayApplication;
import com.cinema.gateway.filter.VerifyJWTFilterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = CinemaGatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.web-application-type=reactive",
                "gateway.auth-service.base-url=http://auth:8081",
                "gateway.services.auth=http://auth:8081",
                "gateway.services.users=http://users:8082",
                "gateway.services.movies=http://movies:8083",
                "gateway.services.cinemas=http://cinemas:8084",
                "gateway.services.tickets=http://tickets:8085",
                "gateway.services.ads=http://ads:8086",
                "gateway.services.reports=http://reports:8087"
        }
)
class GatewayRoutesIntegrationTest {

    @Autowired
    private RouteLocator routeLocator;

    @MockBean
    private VerifyJWTFilterFactory verifyJWTFilterFactory;

    @BeforeEach
    void setUp() {
        GatewayFilter passthrough = (exchange, chain) -> chain.filter(exchange);
        when(verifyJWTFilterFactory.apply(any(VerifyJWTFilterFactory.Config.class))).thenReturn(passthrough);
    }

    @Test
    void shouldRegisterAllGatewayRoutes() {
        List<String> ids = routeLocator.getRoutes().map(Route::getId).collectList().block();

        assertEquals(10, ids.size());
        assertTrue(ids.contains("auth-public-routes"));
        assertTrue(ids.contains("movies-public-routes"));
        assertTrue(ids.contains("movies-api-public-routes"));
        assertTrue(ids.contains("users-private-routes"));
        assertTrue(ids.contains("movies-private-routes"));
        assertTrue(ids.contains("cinemas-private-routes"));
        assertTrue(ids.contains("tickets-private-routes"));
        assertTrue(ids.contains("ads-private-routes"));
        assertTrue(ids.contains("reports-private-routes"));
        verify(verifyJWTFilterFactory, times(6)).apply(any(VerifyJWTFilterFactory.Config.class));
    }
}
