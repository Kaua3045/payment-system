package com.payment.system.domain.transactions;

import java.util.Arrays;
import java.util.Optional;

public enum DepositSource {

    ATM,
    CASH,
    INTERNAL_ADJUSTMENT,
    EXTERNAL;

    public static Optional<DepositSource> from(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
