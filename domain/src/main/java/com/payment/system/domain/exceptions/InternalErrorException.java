package com.payment.system.domain.exceptions;

public class InternalErrorException extends NoStackTraceException {

    private InternalErrorException(final String message, final String errorCode) {
        super(message, errorCode);
    }

    private InternalErrorException(final String message, final Throwable cause, final String errorCode) {
        super(message, cause, errorCode);
    }

    public static InternalErrorException with(final String message, final Throwable cause) {
        return new InternalErrorException(message, cause, "internal_error");
    }

    public static InternalErrorException with(final String message, final Throwable cause, final String errorCode) {
        return new InternalErrorException(message, cause, errorCode);
    }

    public static InternalErrorException with(final String message) {
        return new InternalErrorException(message, "internal_error");
    }

    public static InternalErrorException with(final String message, final String errorCode) {
        return new InternalErrorException(message, errorCode);
    }
}
