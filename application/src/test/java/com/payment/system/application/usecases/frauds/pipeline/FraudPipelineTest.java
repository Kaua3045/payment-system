package com.payment.system.application.usecases.frauds.pipeline;

import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.RequestContext;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FraudPipelineTest extends UnitTest {

    @Test
    void shouldExecuteStepsInOrderAndRecordPath() {
        Attempt attempt = createValidAttempt();

        FraudStep first = new TestStep("first", fraudCase -> {
        });
        FraudStep second = new TestStep("second", fraudCase -> {
            fraudCase.decide(Verdict.APPROVE, 0.1);
        });

        FraudCase result = new FraudPipeline(new ApplicationLogger() {
            @Override
            public void info(String message, Object... args) {

            }

            @Override
            public void warn(String message, Object... args) {

            }

            @Override
            public void error(String message, Object... args) {

            }
        }, List.of(first, second)
        ).run(attempt);

        assertEquals("first>second", result.path());
        assertEquals(Verdict.APPROVE, result.getVerdict());
    }

    @Test
    void shouldStopExecutingStepsAfterDecision() {
        Attempt attempt = createValidAttempt();
        AtomicBoolean thirdStepExecuted = new AtomicBoolean(false);

        FraudStep first = new TestStep("deny", fraudCase ->
                fraudCase.decide(Verdict.DENY, 1.0)
        );

        FraudStep second = new TestStep("never-run", fraudCase ->
                thirdStepExecuted.set(true)
        );

        FraudCase result = new FraudPipeline(new ApplicationLogger() {
            @Override
            public void info(String message, Object... args) {

            }

            @Override
            public void warn(String message, Object... args) {

            }

            @Override
            public void error(String message, Object... args) {

            }
        }, List.of(first, second)
        ).run(attempt);

        assertEquals(Verdict.DENY, result.getVerdict());
        assertEquals("deny", result.path());
        assertFalse(thirdStepExecuted.get());
    }

    @Test
    void shouldApplyFailurePolicyAndRecordFailureInPath() {
        Attempt attempt = createValidAttempt();
        AtomicBoolean failureHandlerCalled = new AtomicBoolean(false);

        FraudStep failingStep = new FraudStep() {
            @Override
            public String name() {
                return "enrich";
            }

            @Override
            public void run(FraudCase fraudCase) {
                throw new IllegalStateException("dependency unavailable");
            }

            @Override
            public void onFailure(
                    FraudCase fraudCase,
                    Exception error
            ) {
                failureHandlerCalled.set(true);
                assertInstanceOf(IllegalStateException.class, error);
                fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);
                fraudCase.setDegraded(true);
            }
        };

        FraudCase result = new FraudPipeline(new ApplicationLogger() {
            @Override
            public void info(String message, Object... args) {

            }

            @Override
            public void warn(String message, Object... args) {

            }

            @Override
            public void error(String message, Object... args) {

            }
        }, List.of(failingStep)
        ).run(attempt);

        assertTrue(failureHandlerCalled.get());
        assertEquals("enrich!", result.path());
        assertEquals(
                List.of("FALLBACK:enrich:IllegalStateException"),
                result.toAssessment().reasons()
        );
        assertEquals(Verdict.MANUAL_REVIEW, result.getVerdict());
        assertTrue(result.isDegraded());
    }

    @Test
    void shouldContinueWhenFailurePolicyDoesNotDecide() {
        Attempt attempt = createValidAttempt();
        AtomicBoolean nextStepExecuted = new AtomicBoolean(false);

        FraudStep failingStep = new FraudStep() {
            @Override
            public String name() {
                return "optional-enrichment";
            }

            @Override
            public void run(FraudCase fraudCase) {
                throw new IllegalArgumentException("invalid optional data");
            }

            @Override
            public void onFailure(
                    FraudCase fraudCase,
                    Exception error
            ) {
                // Deliberately recover without making a decision.
            }
        };

        FraudStep decisionStep = new TestStep("decision", fraudCase -> {
            nextStepExecuted.set(true);
            fraudCase.decide(Verdict.APPROVE, 0.1);
        });

        FraudCase result = new FraudPipeline(new ApplicationLogger() {
            @Override
            public void info(String message, Object... args) {

            }

            @Override
            public void warn(String message, Object... args) {

            }

            @Override
            public void error(String message, Object... args) {

            }
        }, List.of(failingStep, decisionStep)
        ).run(attempt);

        assertTrue(nextStepExecuted.get());
        assertEquals("optional-enrichment!>decision", result.path());
        assertEquals(
                List.of("FALLBACK:optional-enrichment:IllegalArgumentException"),
                result.toAssessment().reasons()
        );
        assertEquals(Verdict.APPROVE, result.getVerdict());
    }

    @Test
    void shouldReturnUndecidedCaseWhenThereAreNoSteps() {
        Attempt attempt = createValidAttempt();

        FraudCase result = new FraudPipeline(new ApplicationLogger() {
            @Override
            public void info(String message, Object... args) {

            }

            @Override
            public void warn(String message, Object... args) {

            }

            @Override
            public void error(String message, Object... args) {

            }
        }, List.of()).run(attempt);

        assertNull(result.getVerdict());
        assertEquals("", result.path());
    }

    private static final class TestStep implements FraudStep {

        private final String name;
        private final java.util.function.Consumer<FraudCase> action;

        private TestStep(
                String name,
                java.util.function.Consumer<FraudCase> action
        ) {
            this.name = name;
            this.action = action;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void run(FraudCase fraudCase) {
            action.accept(fraudCase);
        }

        @Override
        public void onFailure(FraudCase fraudCase, Exception error) {
            fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);
            fraudCase.setDegraded(true);
        }
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
                InstantUtils.now().minus(10, ChronoUnit.DAYS),
                new RequestContext("123.456.789.012")
        );
    }
}
