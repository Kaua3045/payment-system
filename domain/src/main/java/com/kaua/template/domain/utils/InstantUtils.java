package com.kaua.template.domain.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class InstantUtils {

    private static final ChronoUnit TRUNCATE_UNIT = ChronoUnit.MICROS;

    private InstantUtils() {}

    public static Instant now() {
        return Instant.now().truncatedTo(TRUNCATE_UNIT);
    }

    public static Optional<Instant> fromString(final String date) {
        if (date == null || date.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(Instant.parse(date).truncatedTo(TRUNCATE_UNIT));
    }
}
