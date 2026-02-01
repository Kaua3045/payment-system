package com.payment.system.domain.pixkeys;

import java.util.Arrays;
import java.util.Optional;

public enum PixKeyStatus {

    ACTIVE,
    INACTIVE,
    DELETED;

    public static Optional<PixKeyStatus> from(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
