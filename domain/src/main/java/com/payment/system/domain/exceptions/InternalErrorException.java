package com.payment.system.domain.exceptions;

public class InternalErrorException extends NoStackTraceException {

    private InternalErrorException(final String message) {
        super(message);
    }

    private InternalErrorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static InternalErrorException with(final String message, final Throwable cause) {
        return new InternalErrorException(message, cause);
    }

    public static InternalErrorException with(final String message) {
        return new InternalErrorException(message);
    }
}
