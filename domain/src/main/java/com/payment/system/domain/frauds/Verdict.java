package com.payment.system.domain.frauds;

public enum Verdict {

    APPROVE,
    MANUAL_REVIEW,
    DENY;

    // This method determines the verdict based on the provided score and thresholds for review and denial.
    // denyAt: The score threshold above which the transaction is denied. example: denyAt = 0.8, score = 0.9 => DENY
    // reviewAt: The score threshold above which the transaction requires manual review. example: reviewAt = 0.5, score = 0.6 => MANUAL_REVIEW
    // If the score is below both thresholds, the transaction is approved. example: reviewAt = 0.5, score = 0.4 => APPROVE
    public static Verdict of(final double score, final double reviewAt, final double denyAt) {
        if (score >= denyAt) return DENY;
        if (score >= reviewAt) return MANUAL_REVIEW;
        return APPROVE;
    }
}
