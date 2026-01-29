package com.payment.system.domain.exceptions;

public class NoStackTraceException extends RuntimeException {

    public NoStackTraceException(final String message) {
        this(message, null);
    }

    public NoStackTraceException(final String message, final Throwable cause) {
        super(message, cause, true, false);
    }
}
