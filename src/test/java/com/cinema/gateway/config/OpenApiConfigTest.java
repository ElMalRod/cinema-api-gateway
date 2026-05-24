package com.cinema.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void shouldBuildOpenApiWithGatewayMetadataAndPaths() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.gatewayOpenApi();

        assertEquals("Cinema API Gateway", openAPI.getInfo().getTitle());
        assertEquals("v1", openAPI.getInfo().getVersion());
        assertEquals("Cinema Platform Team", openAPI.getInfo().getContact().getName());
        assertEquals("http://localhost:8080", openAPI.getServers().getFirst().getUrl());
        assertNotNull(openAPI.getPaths().get("/auth/{path}"));
        assertNotNull(openAPI.getPaths().get("/users/{path}"));
        assertNotNull(openAPI.getPaths().get("/movies"));
        assertNotNull(openAPI.getPaths().get("/movies/{id}"));
        assertNotNull(openAPI.getPaths().get("/movies/{path}"));
        assertNotNull(openAPI.getPaths().get("/cinemas/{path}"));
        assertNotNull(openAPI.getPaths().get("/tickets/{path}"));
        assertNotNull(openAPI.getPaths().get("/ads/{path}"));
        assertNotNull(openAPI.getPaths().get("/reports/{path}"));
    }

    @Test
    void shouldConfigureHttpOperationsAndDefaultResponses() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.gatewayOpenApi();

        PathItem moviesPath = openAPI.getPaths().get("/movies");
        assertNotNull(moviesPath);

        Operation get = moviesPath.getGet();
        Operation post = moviesPath.getPost();
        Operation put = moviesPath.getPut();
        Operation patch = moviesPath.getPatch();
        Operation delete = moviesPath.getDelete();

        assertNotNull(get);
        assertNotNull(post);
        assertNotNull(put);
        assertNotNull(patch);
        assertNotNull(delete);
        assertTrue(get.getResponses().containsKey("200"));
        assertTrue(get.getResponses().containsKey("401"));
        assertTrue(get.getResponses().containsKey("403"));
        assertTrue(get.getResponses().containsKey("404"));
        assertTrue(get.getResponses().containsKey("500"));
        assertEquals("object", get.getResponses().get("200")
                .getContent()
                .get("application/json")
                .getSchema()
                .getType());
    }
}
