package com.payment.system.domain.exceptions;

import com.payment.system.domain.validation.Error;

import java.util.Collections;
import java.util.List;

public class ConflictException extends NoStackTraceException {

    private final List<Error> errors;

    private ConflictException(final String message, final List<Error> errors, final String errorCode) {
        super(message, errorCode);
        this.errors = errors;
    }

    public static ConflictException with(final String message, final String errorCode) {
        return new ConflictException(message, Collections.emptyList(), errorCode);
    }

    public static ConflictException with(final List<Error> errors, final String errorCode) {
        return new ConflictException("ConflictException", errors, errorCode);
    }

    public List<Error> getErrors() {
        return errors;
    }
}
