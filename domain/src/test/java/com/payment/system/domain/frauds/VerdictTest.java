package com.payment.system.domain.frauds;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VerdictTest extends UnitTest {

    @Test
    void givenAnValidApproveScore_whenOf_thenShouldReturnApprove() {
        final var score = 0.4;
        final var reviewAt = 0.5;
        final var denyAt = 0.8;

        final var verdict = Verdict.of(score, reviewAt, denyAt);

        Assertions.assertEquals(Verdict.APPROVE, verdict);
    }

    @Test
    void givenAnValidManualReviewScore_whenOf_thenShouldReturnManualReview() {
        final var score = 0.6;
        final var reviewAt = 0.5;
        final var denyAt = 0.8;

        final var verdict = Verdict.of(score, reviewAt, denyAt);

        Assertions.assertEquals(Verdict.MANUAL_REVIEW, verdict);
    }

    @Test
    void givenAnValidDenyScore_whenOf_thenShouldReturnDeny() {
        final var score = 0.9;
        final var reviewAt = 0.5;
        final var denyAt = 0.8;

        final var verdict = Verdict.of(score, reviewAt, denyAt);

        Assertions.assertEquals(Verdict.DENY, verdict);
    }
}
