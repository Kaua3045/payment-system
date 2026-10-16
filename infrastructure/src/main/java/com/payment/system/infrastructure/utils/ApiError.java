package com.payment.system.infrastructure.utils;

import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.validation.Error;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record ApiError(String message, List<Error> errors, String errorCode, Instant timestamp) {

    public static ApiError from(final DomainException ex, final Instant timestamp) {
        return new ApiError(ex.getMessage(), ex.getErrors(), ex.getErrorCode(), timestamp);
    }

    public static ApiError from(final String message, final String errorCode, final Instant timestamp) {
        return new ApiError(message, Collections.emptyList(), errorCode, timestamp);
    }
}
