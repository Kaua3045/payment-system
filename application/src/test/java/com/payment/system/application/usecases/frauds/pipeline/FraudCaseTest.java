package com.payment.system.application.usecases.frauds.pipeline;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.features.FeatureVector;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FraudCaseTest extends UnitTest {

    private Attempt attempt;
    private FraudCase fraudCase;

    @BeforeEach
    void setUp() {
        attempt = createValidAttempt();
        fraudCase = new FraudCase(attempt);
    }

    @Test
    void shouldPreserveAttempt() {
        assertSame(attempt, fraudCase.getAttempt());
    }

    @Test
    void shouldNotBeDecidedInitially() {
        assertNull(fraudCase.getVerdict());
        assertTrue(Double.isNaN(fraudCase.getScore()));
    }

    @Test
    void shouldStoreDecision() {
        fraudCase.decide(Verdict.DENY, 1.0);

        assertEquals(Verdict.DENY, fraudCase.getVerdict());
        assertEquals(1.0, fraudCase.getScore());
        assertTrue(fraudCase.getReasons().isEmpty());
    }

    @Test
    void shouldAllowUpdatingDecision() {
        fraudCase.decide(Verdict.APPROVE, 0.2);
        fraudCase.decide(Verdict.DENY, 1.0);

        assertEquals(Verdict.DENY, fraudCase.getVerdict());
        assertEquals(1.0, fraudCase.getScore());
    }

    @Test
    void shouldThrowWhenConvertingUndecidedCaseToAssessment() {
        assertThrows(
                InternalErrorException.class,
                () -> fraudCase.toAssessment()
        );
    }

    @Test
    void shouldCreateAssessmentFromDecision() {
        fraudCase.addReason("HIGH_RISK");
        fraudCase.decide(Verdict.DENY, 1.0);

        Assessment assessment = fraudCase.toAssessment();

        assertEquals(Verdict.DENY, assessment.verdict());
        assertEquals(1.0, assessment.score());
        assertEquals(List.of("HIGH_RISK"), assessment.reasons());
        assertFalse(assessment.degraded());
    }

    @Test
    void shouldPreserveMultipleReasonsInOrder() {
        fraudCase.addReason("HIGH_AMOUNT");
        fraudCase.addReason("SUSPICIOUS_DEVICE");
        fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);

        Assessment assessment = fraudCase.toAssessment();

        assertEquals(
                List.of("HIGH_AMOUNT", "SUSPICIOUS_DEVICE"),
                assessment.reasons()
        );
    }

    @Test
    void shouldReturnEmptyPathInitially() {
        assertEquals("", fraudCase.path());
    }

    @Test
    void shouldStoreAndReturnStats() {
        var stats = com.payment.system.domain.frauds.Stats.empty();

        fraudCase.setStats(stats);

        assertSame(stats, fraudCase.getStats());
    }

    @Test
    void shouldAccumulateBoostLogitIncrements() {
        fraudCase.boostLogitIncrement(0.4);
        fraudCase.boostLogitIncrement(0.3);

        assertEquals(0.7, fraudCase.getBoostLogit(), 1e-9);
    }

    @Test
    void shouldStoreScore() {
        fraudCase.setScore(0.85);

        assertEquals(0.85, fraudCase.getScore());
    }

    @Test
    void shouldStoreVerdict() {
        fraudCase.setVerdict(Verdict.MANUAL_REVIEW);

        assertEquals(Verdict.MANUAL_REVIEW, fraudCase.getVerdict());
    }

    @Test
    void shouldStoreDegradedState() {
        fraudCase.setDegraded(true);

        assertTrue(fraudCase.isDegraded());
    }

    @Test
    void shouldIncludeDegradedStateInAssessment() {
        fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);
        fraudCase.setDegraded(true);

        Assessment assessment = fraudCase.toAssessment();

        assertTrue(assessment.degraded());
    }

    @Test
    void shouldReturnFalseWhenNotDecided() {
        assertFalse(fraudCase.isDecided());
    }

    @Test
    void shouldReturnTrueWhenDecided() {
        fraudCase.decide(Verdict.DENY, 1.0);

        assertTrue(fraudCase.isDecided());
    }

    @Test
    void shouldReturnFeaturesAfterSettingThem() {
        FeatureVector features = FeatureVector.builder().build();

        fraudCase.setFeatures(features);

        assertSame(features, fraudCase.getFeatures());
    }

    private Attempt createValidAttempt() {
        return Attempt.pix(
                "idempotency",
                "account-from",
                "account-to",
                "273712783712",
                InstantUtils.now().minus(10, ChronoUnit.HOURS),
                Money.of(new BigDecimal("500.00")),
                InstantUtils.now(),
                null,
                InstantUtils.now().minus(10, ChronoUnit.DAYS)
        );
    }
}
