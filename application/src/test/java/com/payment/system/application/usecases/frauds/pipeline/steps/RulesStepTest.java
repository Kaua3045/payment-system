package com.payment.system.application.usecases.frauds.pipeline.steps;

import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudPipeline;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.features.FeatureVector;
import com.payment.system.domain.frauds.rules.RuleResult;
import com.payment.system.domain.frauds.rules.Rules;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RulesStepTest extends UnitTest {

    private static final Instant NOW =
            Instant.parse("2026-09-30T15:00:00Z");

    private static Attempt attempt() {
        return Attempt.pix(
                "attempt-1",
                "a",
                "b",
                "b@pix",
                NOW,
                Money.of(new BigDecimal("100.00")),
                NOW,
                null,
                NOW
        );
    }

    private static FraudCase createFraudCase(Attempt attempt) {
        AtomicReference<FraudCase> captured = new AtomicReference<>();

        FraudStep captureStep = new FraudStep() {
            @Override
            public String name() {
                return "capture";
            }

            @Override
            public void run(FraudCase fraudCase) {
                captured.set(fraudCase);
                fraudCase.decide(Verdict.MANUAL_REVIEW, 0.0);
            }

            @Override
            public void onFailure(FraudCase fraudCase, Exception error) {
                throw new AssertionError(error);
            }
        };

        new FraudPipeline(List.of(captureStep)).run(attempt);

        return Objects.requireNonNull(captured.get());
    }

    @Test
    void nameShouldReturnRules() {
        RulesStep step = new RulesStep(List.of());

        assertEquals("rules", step.name());
    }

    @Test
    void givenNoRules_whenRun_shouldLeaveCaseUndecided() throws Exception {
        FraudCase fraudCase = createFraudCase(attempt());
        RulesStep step = new RulesStep(List.of());

        step.run(fraudCase);

        assertEquals(Verdict.MANUAL_REVIEW, fraudCase.getVerdict());
        assertEquals(0.0, fraudCase.getBoostLogit());
    }

    @Test
    void givenDenyRule_whenRun_shouldDenyCase() throws Exception {
        var rule = Rules.blocklist(Set.of("acc:a")::contains);
        FraudCase fraudCase = createFraudCase(attempt());

        new RulesStep(List.of(rule)).run(fraudCase);

        assertEquals(Verdict.DENY, fraudCase.getVerdict());
        assertEquals(1.0, fraudCase.getScore());
        assertTrue(fraudCase.getReasons().contains("BLOCKLIST_ORIGIN"));
    }

    @Test
    void givenDenyRule_whenRun_shouldStopEvaluatingSubsequentRules()
            throws Exception {
        AtomicBoolean secondRuleExecuted = new AtomicBoolean(false);

        Rules.Rule denyRule = Rules.blocklist(Set.of("acc:a")::contains);
        Rules.Rule secondRule = (attempt, features) -> {
            secondRuleExecuted.set(true);
            return RuleResult.PASS;
        };

        FraudCase fraudCase = createFraudCase(attempt());

        new RulesStep(List.of(denyRule, secondRule)).run(fraudCase);

        assertEquals(Verdict.DENY, fraudCase.getVerdict());
        assertFalse(secondRuleExecuted.get());
    }

    @Test
    void givenBoostRule_whenRun_shouldAccumulateLogitAndReason()
            throws Exception {
        FeatureVector features = FeatureVector.builder()
                .set(FeatureVector.COUNT_1M_LOG, 1.5)
                .build();

        FraudCase fraudCase = createFraudCase(attempt());
        fraudCase.setFeatures(features);

        Rules.Rule rule = Rules.featureAtLeast(
                FeatureVector.COUNT_1M_LOG,
                1.0,
                "VELOCITY_1M",
                2.0
        );

        new RulesStep(List.of(rule)).run(fraudCase);

        assertEquals(2.0, fraudCase.getBoostLogit());
        assertTrue(fraudCase.getReasons().contains("VELOCITY_1M"));
        assertEquals(Verdict.MANUAL_REVIEW, fraudCase.getVerdict());
    }

    @Test
    void givenMultipleBoostRules_whenRun_shouldAccumulateTheirLogits()
            throws Exception {
        FeatureVector features = FeatureVector.builder()
                .set(FeatureVector.COUNT_1M_LOG, 2.0)
                .build();

        FraudCase fraudCase = createFraudCase(attempt());
        fraudCase.setFeatures(features);

        Rules.Rule first = Rules.featureAtLeast(
                FeatureVector.COUNT_1M_LOG, 1.0, "VELOCITY_1M", 1.5
        );

        Rules.Rule second = Rules.featureAtLeast(
                FeatureVector.COUNT_1M_LOG, 1.0, "VELOCITY_5M", 2.5
        );

        new RulesStep(List.of(first, second)).run(fraudCase);

        assertEquals(4.0, fraudCase.getBoostLogit());
        assertTrue(fraudCase.getReasons().contains("VELOCITY_1M"));
        assertTrue(fraudCase.getReasons().contains("VELOCITY_5M"));
    }

    @Test
    void givenRuleFailure_whenOnFailure_shouldRequireManualReview() {
        FraudCase fraudCase = createFraudCase(attempt());

        new RulesStep(List.of()).onFailure(
                fraudCase,
                new RuntimeException("rule failure")
        );

        assertEquals(Verdict.MANUAL_REVIEW, fraudCase.getVerdict());
        assertTrue(Double.isNaN(fraudCase.getScore()));
        assertTrue(fraudCase.isDegraded());
    }
}
