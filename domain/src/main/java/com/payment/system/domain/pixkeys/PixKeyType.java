package com.payment.system.domain.pixkeys;

import java.util.Arrays;
import java.util.Optional;

public enum PixKeyType {

    CPF,
    CNPJ,
    EMAIL,
    PHONE,
    RANDOM;

    public static Optional<PixKeyType> from(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
