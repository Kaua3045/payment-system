package com.payment.system.domain.accounts;

import java.util.Arrays;
import java.util.Optional;

public enum AccountStatus {

    ACTIVE,
    BLOCKED,
    CLOSED;

    public static Optional<AccountStatus> from(final String aValue) {
        return Arrays.stream(values())
                .filter(it -> it.name().equalsIgnoreCase(aValue))
                .findFirst();
    }
}
