package com.kaua.template.infrastructure.exceptions;

import com.kaua.template.domain.exceptions.NoStackTraceException;

public class IdempotencyKeyUnsupportedMethodException extends NoStackTraceException {

    public IdempotencyKeyUnsupportedMethodException(final String method) {
        super("Idempotency key is not supported for this method: " + method);
    }
}
