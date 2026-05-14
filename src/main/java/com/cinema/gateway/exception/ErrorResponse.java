package com.cinema.gateway.exception;

import java.time.Instant;
import java.util.Objects;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Instant timestamp;
        private Integer status;
        private String message;
        private String path;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = Objects.requireNonNull(message, "message is required");
            return this;
        }

        public Builder path(String path) {
            this.path = Objects.requireNonNull(path, "path is required");
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(
                    Objects.requireNonNull(timestamp, "timestamp is required"),
                    Objects.requireNonNull(status, "status is required"),
                    Objects.requireNonNull(message, "message is required"),
                    Objects.requireNonNull(path, "path is required")
            );
        }
    }
}
