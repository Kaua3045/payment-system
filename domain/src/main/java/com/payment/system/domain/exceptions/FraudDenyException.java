package com.payment.system.domain.exceptions;

import java.util.List;

public class FraudDenyException extends DomainException {

    protected FraudDenyException(String message) {
        super(message, List.of(), "FRAUD_DENY");
    }
}
