package com.cinema.gateway;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class CinemaGatewayApplicationTest {

    @Test
    void shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            String[] args = new String[]{"--spring.main.banner-mode=off"};

            CinemaGatewayApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(CinemaGatewayApplication.class, args));
        }
    }
}
