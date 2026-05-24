package com.cinema.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayConfigTest {

    @Test
    void shouldCreateSecurityWebFilterChain() {
        GatewayConfig config = new GatewayConfig();
        ServerHttpSecurity http = ServerHttpSecurity.http();

        SecurityWebFilterChain chain = config.securityWebFilterChain(http);

        assertNotNull(chain);
    }
}
