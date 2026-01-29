package com.payment.system.infrastructure.exceptions;

import com.payment.system.domain.exceptions.DomainException;

import java.util.Collections;

public class IdempotencyKeyAlreadyExistsException extends DomainException {

    public IdempotencyKeyAlreadyExistsException() {
        super("Idempotency key already exists", Collections.emptyList());
    }
}
