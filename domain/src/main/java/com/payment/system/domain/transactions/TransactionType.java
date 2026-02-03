package com.payment.system.domain.transactions;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionType {

    CREDIT,
    DEBIT,
    TRANSFER;

    public static Optional<TransactionType> from(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
