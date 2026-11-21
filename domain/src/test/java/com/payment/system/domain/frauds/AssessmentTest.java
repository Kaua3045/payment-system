package com.payment.system.domain.frauds;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class AssessmentTest extends UnitTest {

    @Test
    void givenAnValidAssessment_whenCreated_thenShouldBeEqual() {
        final var assessment = new Assessment(Verdict.APPROVE, 0.4, List.of("Reason 1", "Reason 2"));

        Assertions.assertEquals(Verdict.APPROVE, assessment.verdict());
        Assertions.assertEquals(0.4, assessment.score());
        Assertions.assertEquals(List.of("Reason 1", "Reason 2"), assessment.reasons());
        Assertions.assertFalse(assessment.degraded());
    }

    @Test
    void givenAnValidAssessmentWithDegraded_whenCreated_thenShouldBeEqual() {
        // why = "Some reason for fallback"
        final var assessment = Assessment.fallback(Verdict.DENY, "Some reason for fallback");

        Assertions.assertEquals(Verdict.DENY, assessment.verdict());
        Assertions.assertEquals(Double.NaN, assessment.score());
        Assertions.assertEquals(List.of("FALLBACK:Some reason for fallback"), assessment.reasons());
        Assertions.assertTrue(assessment.degraded());
    }
}
