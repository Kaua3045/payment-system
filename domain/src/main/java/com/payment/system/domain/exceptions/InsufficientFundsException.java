package com.payment.system.domain.exceptions;

import java.util.ArrayList;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException() {
        super("Insufficient funds", new ArrayList<>());
    }

    public InsufficientFundsException(String complement) {
        super("Insufficient funds %s".formatted(complement), new ArrayList<>());
    }
}
