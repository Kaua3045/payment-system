package com.payment.system.domain.exceptions;

import com.payment.system.domain.validation.Error;

import java.util.List;

public class DomainException extends NoStackTraceException {

    private static final String DOMAIN_ERROR_CODE = "domain_rule_violation";

    protected final List<Error> errors;

    protected DomainException(final String message, final List<Error> anErrors, final String errorCode) {
        super(message, errorCode);
        this.errors = anErrors;
    }

    public static DomainException with(final String aMessage) {
        return new DomainException(aMessage, List.of(), DOMAIN_ERROR_CODE);
    }

    public static DomainException with(final List<Error> aErrors) {
        return new DomainException("DomainException", aErrors, DOMAIN_ERROR_CODE);
    }

    public static DomainException with(final Error aError) {
        return new DomainException("DomainException", List.of(aError), DOMAIN_ERROR_CODE);
    }

    public static DomainException with(final String aMessage, final List<Error> aErrors) {
        return new DomainException(aMessage, aErrors, DOMAIN_ERROR_CODE);
    }

    public List<Error> getErrors() {
        return this.errors;
    }
}
