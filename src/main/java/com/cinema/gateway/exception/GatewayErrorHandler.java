package com.cinema.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatus status = resolveStatus(ex);
        String message = ex.getMessage() == null ? "Unexpected gateway error" : ex.getMessage();
        ErrorResponse body = buildError(status, message, exchange.getRequest().getPath().value());
        log.warn("Gateway error status={} path={} message={}", status.value(), body.path(), body.message());
        return writeResponse(exchange.getResponse(), status, body);
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof GatewayAuthException authException) {
            return authException.getStatus();
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ErrorResponse buildError(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .message(message)
                .path(path)
                .build();
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, HttpStatus status, ErrorResponse body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(serializeBody(body));
        return response.writeWith(Mono.just(buffer));
    }

    private byte[] serializeBody(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception exception) {
            String fallback = "{\"status\":500,\"message\":\"Error serializing response\",\"path\":\"/\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
