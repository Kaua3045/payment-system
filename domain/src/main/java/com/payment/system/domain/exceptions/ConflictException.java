package com.payment.system.domain.exceptions;

import com.payment.system.domain.validation.Error;

import java.util.Collections;
import java.util.List;

public class ConflictException extends NoStackTraceException {

    private final List<Error> errors;

    private ConflictException(final String message, final List<Error> errors) {
        super(message);
        this.errors = errors;
    }

    public static ConflictException with(final String message) {
        return new ConflictException(message, Collections.emptyList());
    }

    public static ConflictException with(final List<Error> errors) {
        return new ConflictException("ConflictException", errors);
    }

    public List<Error> getErrors() {
        return errors;
    }
}
