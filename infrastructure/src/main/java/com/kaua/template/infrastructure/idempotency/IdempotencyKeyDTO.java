package com.kaua.template.infrastructure.idempotency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

public record IdempotencyKeyDTO(int statusCode, String body, Map<String, String> headers) implements Serializable {

    @JsonCreator
    public IdempotencyKeyDTO(
            @JsonProperty("status_code") final int statusCode,
            @JsonProperty("body") final String body,
            @JsonProperty("headers") Map<String, String> headers
    ) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
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
