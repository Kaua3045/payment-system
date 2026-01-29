package com.payment.system.application.exceptions;

import com.payment.system.domain.exceptions.NoStackTraceException;

public class UseCaseInputCannotBeNullException extends NoStackTraceException {

    public UseCaseInputCannotBeNullException(Class<?> clazz) {
        super("Input to %s cannot be null".formatted(clazz.getSimpleName()));
    }
}
