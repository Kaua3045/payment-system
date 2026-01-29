package com.kaua.template.domain.utils;

import com.kaua.template.domain.validation.AssertionConcern;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record Period(
        Instant start,
        Instant end
) implements AssertionConcern {

    public Period {
        this.assertArgumentNotNull(start, "start", "should not be null");
        this.assertArgumentNotNull(end, "end", "should not be null");
        this.assertArgumentTrue(start.compareTo(end) < 0, "start", "should be before end");
    }

    public Period(final String start, final String end) {
        this(
                InstantUtils.fromString(start).orElse(null),
                InstantUtils.fromString(end).orElse(null)
        );
    }

    public static Instant startValidate(final String start, final int minus, final ChronoUnit chronoUnit) {
        return InstantUtils.fromString(start)
                .orElse(InstantUtils.now().minus(minus, chronoUnit));
    }

    public static Instant endValidate(final String end, final int plus, final ChronoUnit chronoUnit) {
        return InstantUtils.fromString(end)
                .orElse(InstantUtils.now().plus(plus, chronoUnit));
    }
}