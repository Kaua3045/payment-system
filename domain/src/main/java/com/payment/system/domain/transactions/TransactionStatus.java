package com.payment.system.domain.transactions;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionStatus {

    PENDING,
    COMPLETED,
    FAILED;

    public static Optional<TransactionStatus> from(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
