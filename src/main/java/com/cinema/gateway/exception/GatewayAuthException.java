package com.cinema.gateway.exception;

import org.springframework.http.HttpStatus;

public class GatewayAuthException extends RuntimeException {

    private final HttpStatus status;

    public GatewayAuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
