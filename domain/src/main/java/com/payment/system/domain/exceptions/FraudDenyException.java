package com.payment.system.domain.exceptions;

import java.util.List;

public class FraudDenyException extends DomainException {

    public FraudDenyException() {
        super("Transaction rejected by fraud analysis", List.of(), "FRAUD_DENY");
    }
}
