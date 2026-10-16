package com.payment.system.domain.exceptions;

public class NoStackTraceException extends RuntimeException {

    private final String errorCode;

    public NoStackTraceException(final String message, final String errorCode) {
        this(message, null, errorCode);
    }

    public NoStackTraceException(final String message, final Throwable cause, final String errorCode) {
        super(message, cause, true, false);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
