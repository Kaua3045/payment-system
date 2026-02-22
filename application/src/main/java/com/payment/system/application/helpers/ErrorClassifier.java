package com.payment.system.application.helpers;

import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;

public final class ErrorClassifier {

    private ErrorClassifier() {
    }

    public static ErrorType classify(final Exception ex) {
        if (ex instanceof DomainException || ex instanceof NotFoundException) {
            return ErrorType.BUSINESS;
        }
        return ErrorType.UNEXPECTED;
    }
}
