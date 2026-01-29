package com.payment.system.infrastructure.exceptions;

import com.payment.system.domain.exceptions.DomainException;

import java.util.Collections;

public class IdempotencyKeyRequiredException extends DomainException {

    public IdempotencyKeyRequiredException() {
        super("Idempotency key required and the required header is 'x-idempotency-key'", Collections.emptyList());
    }
}
