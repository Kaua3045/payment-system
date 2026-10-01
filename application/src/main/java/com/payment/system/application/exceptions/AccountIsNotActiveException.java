package com.payment.system.application.exceptions;

import com.payment.system.domain.exceptions.DomainException;

import java.util.ArrayList;

public class AccountIsNotActiveException extends DomainException {

    public AccountIsNotActiveException(final String account) {
        super("Account %s is not active".formatted(account), new ArrayList<>());
    }
}
