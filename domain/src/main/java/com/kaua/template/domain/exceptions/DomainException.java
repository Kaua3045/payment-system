package com.kaua.template.domain.exceptions;

import com.kaua.template.domain.validation.Error;

import java.util.List;

public class DomainException extends NoStackTraceException {

    protected final List<Error> errors;

    protected DomainException(final String message, final List<Error> anErrors) {
        super(message);
        this.errors = anErrors;
    }

    public static DomainException with(final String aMessage) {
        return new DomainException(aMessage, List.of());
    }

    public static DomainException with(final List<Error> aErrors) {
        return new DomainException("DomainException", aErrors);
    }

    public static DomainException with(final Error aError) {
        return new DomainException("DomainException", List.of(aError));
    }

    public static DomainException with(final String aMessage, final List<Error> aErrors) {
        return new DomainException(aMessage, aErrors);
    }

    public List<Error> getErrors() {
        return this.errors;
    }
}
