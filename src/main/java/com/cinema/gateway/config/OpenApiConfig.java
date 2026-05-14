package com.cinema.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cinema API Gateway")
                        .description("Single entry point for cinema microservices")
                        .version("v1")
                        .contact(new Contact().name("Cinema Platform Team")))
                .servers(List.of(new Server().url("http://localhost:8080")))
                .paths(proxyPaths());
    }

    private Paths proxyPaths() {
        Paths paths = new Paths();
        paths.addPathItem("/auth/{path}", proxyPath("Auth service proxy"));
        paths.addPathItem("/users/{path}", proxyPath("Users service proxy"));
        paths.addPathItem("/movies", proxyPath("Movies service proxy - public listing"));
        paths.addPathItem("/movies/{id}", proxyPath("Movies service proxy - public detail"));
        paths.addPathItem("/movies/{path}", proxyPath("Movies service proxy"));
        paths.addPathItem("/cinemas/{path}", proxyPath("Cinemas service proxy"));
        paths.addPathItem("/tickets/{path}", proxyPath("Tickets service proxy"));
        paths.addPathItem("/ads/{path}", proxyPath("Ads service proxy"));
        paths.addPathItem("/reports/{path}", proxyPath("Reports service proxy"));
        return paths;
    }

    private PathItem proxyPath(String summary) {
        Operation operation = new Operation()
                .summary(summary)
                .description("Request is proxied by API Gateway to target microservice")
                .responses(defaultResponses());
        return new PathItem()
                .get(operation)
                .post(operation)
                .put(operation)
                .patch(operation)
                .delete(operation);
    }

    private ApiResponses defaultResponses() {
        return new ApiResponses()
                .addApiResponse("200", jsonResponse("Request accepted and forwarded"))
                .addApiResponse("401", jsonResponse("Unauthorized"))
                .addApiResponse("403", jsonResponse("Forbidden"))
                .addApiResponse("404", jsonResponse("Route not found"))
                .addApiResponse("500", jsonResponse("Gateway internal error"));
    }

    private ApiResponse jsonResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().type("object"))
                ));
    }
}
