package com.payment.system.application.usecases.frauds.pipeline.steps;

import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudPipeline;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.features.FeatureVector;
import com.payment.system.domain.frauds.model.Sigmoid;
import com.payment.system.domain.frauds.model.TreeModel;
import com.payment.system.domain.frauds.model.TreeModelParser;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ScoreStepTest extends UnitTest {

    private static final Instant NOW =
            Instant.parse("2026-09-30T15:00:00Z");

    private static final double REVIEW_AT = 0.30;
    private static final double DENY_AT = 0.70;

    private static final BigDecimal FAIL_OPEN_BELOW =
            new BigDecimal("100.00");

    private static Attempt attempt(BigDecimal amount) {
        return Attempt.pix(
                "attempt-1",
                "a",
                "b",
                "b@pix",
                NOW,
                Money.of(amount),
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
            public void onFailure(
                    FraudCase fraudCase,
                    Exception error
            ) {
                throw new AssertionError(error);
            }
        };

        new FraudPipeline(List.of(captureStep)).run(attempt);

        return Objects.requireNonNull(captured.get());
    }

    private static TreeModel modelReturning(double rawScore)
            throws Exception {
        String content = """
                TREEMODEL 1
                FEATURES %s
                BASE %s
                TREES 1
                TREE 1
                0 L 0.0
                END
                """.formatted(
                String.join(",", FeatureVector.NAMES),
                Double.toString(rawScore)
        );

        return TreeModelParser.parse(new StringReader(content));
    }

    private static TreeModel modelWithOverflow() throws Exception {
        String content = """
                TREEMODEL 1
                FEATURES %s
                BASE 1.0E308
                TREES 1
                TREE 1
                0 L 1.0E308
                END
                """.formatted(String.join(",", FeatureVector.NAMES));

        return TreeModelParser.parse(new StringReader(content));
    }

    private static ScoreStep createStep(TreeModel model) {
        return new ScoreStep(
                model,
                REVIEW_AT,
                DENY_AT,
                FAIL_OPEN_BELOW
        );
    }

    private static FraudCase newCase(BigDecimal amount) {
        return createFraudCase(attempt(amount));
    }

    // name

    @Test
    void nameShouldReturnScore() throws Exception {
        ScoreStep step = createStep(modelReturning(0.0));

        assertEquals("score", step.name());
    }

    // run: successful scoring

    @Test
    void givenLowScore_whenRun_shouldApprove()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("100.00"));
        ScoreStep step = createStep(modelReturning(-10.0));

        step.run(fraudCase);

        assertEquals(Verdict.APPROVE, fraudCase.getVerdict());
        assertFalse(fraudCase.isDegraded());
        assertEquals(
                Sigmoid.sigmoid(-10.0),
                fraudCase.getScore(),
                1e-12
        );
    }

    @Test
    void givenIntermediateScore_whenRun_shouldRequireManualReview()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("100.00"));
        ScoreStep step = createStep(modelReturning(0.0));

        step.run(fraudCase);

        assertEquals(Verdict.MANUAL_REVIEW, fraudCase.getVerdict());
        assertFalse(fraudCase.isDegraded());
        assertEquals(0.5, fraudCase.getScore(), 1e-12);
    }

    @Test
    void givenHighScore_whenRun_shouldDeny()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("100.00"));
        ScoreStep step = createStep(modelReturning(10.0));

        step.run(fraudCase);

        assertEquals(Verdict.DENY, fraudCase.getVerdict());
        assertFalse(fraudCase.isDegraded());
        assertEquals(
                Sigmoid.sigmoid(10.0),
                fraudCase.getScore(),
                1e-12
        );
    }

    // run: invalid raw model score

    @Test
    void givenOverflowingRawScore_whenRun_shouldThrowInternalError()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("100.00"));
        ScoreStep step = createStep(modelWithOverflow());

        assertThrows(
                InternalErrorException.class,
                () -> step.run(fraudCase)
        );
    }

    // onFailure: fail-open policy

    @Test
    void givenAmountBelowFailOpenLimit_whenOnFailure_shouldApprove()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("50.00"));
        ScoreStep step = createStep(modelReturning(0.0));

        step.onFailure(
                fraudCase,
                new RuntimeException("model unavailable")
        );

        assertEquals(Verdict.APPROVE, fraudCase.getVerdict());
        assertTrue(fraudCase.isDegraded());
        assertTrue(Double.isNaN(fraudCase.getScore()));
    }

    @Test
    void givenAmountEqualToFailOpenLimit_whenOnFailure_shouldApprove()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("100.00"));
        ScoreStep step = createStep(modelReturning(0.0));

        step.onFailure(
                fraudCase,
                new RuntimeException("model unavailable")
        );

        assertEquals(Verdict.APPROVE, fraudCase.getVerdict());
        assertTrue(fraudCase.isDegraded());
        assertTrue(Double.isNaN(fraudCase.getScore()));
    }

    @Test
    void givenAmountAboveFailOpenLimit_whenOnFailure_shouldRequireManualReview()
            throws Exception {
        FraudCase fraudCase = newCase(new BigDecimal("100.01"));
        ScoreStep step = createStep(modelReturning(0.0));

        step.onFailure(
                fraudCase,
                new RuntimeException("model unavailable")
        );

        assertEquals(
                Verdict.MANUAL_REVIEW,
                fraudCase.getVerdict()
        );
        assertTrue(fraudCase.isDegraded());
        assertTrue(Double.isNaN(fraudCase.getScore()));
    }
}
