package com.payment.system.application.exceptions;

import com.payment.system.domain.exceptions.DomainException;

import java.util.ArrayList;

public class PixKeyIsNotActiveException extends DomainException {

    public PixKeyIsNotActiveException(final String pixKey) {
        super("PixKey %s is not active".formatted(pixKey), new ArrayList<>());
    }
}
