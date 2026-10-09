package com.payment.system.domain.exceptions;

import java.util.ArrayList;

public class AccountIsNotActiveException extends DomainException {

    public AccountIsNotActiveException(final String account) {
        super("Account %s is not active".formatted(account), new ArrayList<>(), "account_inactive");
    }
}
