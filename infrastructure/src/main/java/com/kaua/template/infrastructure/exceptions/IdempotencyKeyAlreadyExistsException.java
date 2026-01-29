package com.kaua.template.infrastructure.exceptions;

import com.kaua.template.domain.exceptions.DomainException;

import java.util.Collections;

public class IdempotencyKeyAlreadyExistsException extends DomainException {

    public IdempotencyKeyAlreadyExistsException() {
        super("Idempotency key already exists", Collections.emptyList());
    }
}
