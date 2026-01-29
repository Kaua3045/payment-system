package com.kaua.template.application.exceptions;

import com.kaua.template.domain.exceptions.NoStackTraceException;

public class UseCaseInputCannotBeNullException extends NoStackTraceException {

    public UseCaseInputCannotBeNullException(String useCaseName) {
        super("Input to %s cannot be null".formatted(useCaseName));
    }
}
