package com.kaua.template.infrastructure.idempotency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

public record IdempotencyKeyInput(int statusCode, Object body, Map<String, String> headers) implements Serializable {

    @JsonCreator
    public IdempotencyKeyInput(
            @JsonProperty("status_code") final int statusCode,
            @JsonProperty("body") final Object body,
            @JsonProperty("headers") Map<String, String> headers
    ) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    public IdempotencyKeyInput(final Object body) {
        this(0, body, Map.of());
    }

    @Override
    public String toString() {
        return "IdempotencyKeyInput(" +
                "statusCode=" + statusCode +
                ", body=" + body +
                ", headers=" + headers.size() +
                ')';
    }
}
