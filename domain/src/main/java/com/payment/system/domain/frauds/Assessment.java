package com.payment.system.domain.frauds;

import java.util.List;

public record Assessment(Verdict verdict, double score, List<String> reasons, boolean degraded) {

    public Assessment {
        reasons = List.copyOf(reasons);
    }

    public Assessment(final Verdict verdict, final double score, final List<String> reasons) {
        this(verdict, score, reasons, false);
    }

    // This method creates a fallback Assessment with a given verdict and reason for fallback.
    // The score is set to NaN to indicate that it is not applicable in this context.
    // The reasons list contains a single entry indicating that this is a fallback assessment and includes the provided reason.
    public static Assessment fallback(final Verdict verdict, final String why) {
        return new Assessment(verdict, Double.NaN, List.of("FALLBACK:" + why), true);
    }
}
