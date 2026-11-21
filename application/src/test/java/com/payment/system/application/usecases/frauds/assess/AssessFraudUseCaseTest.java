package com.payment.system.application.usecases.frauds.assess;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudPipeline;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.frauds.*;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class AssessFraudUseCaseTest extends UseCaseTest {

    @Mock
    StatsRepository statsRepository;

    private DefaultAssessFraudUseCase useCase;

    @BeforeEach
    void setUp() {
        FraudPipeline pipeline = new FraudPipeline(applicationLogger, List.of());
        useCase = new DefaultAssessFraudUseCase(
                applicationLogger,
                Map.of(Channel.PIX, pipeline),
                statsRepository,
                metrics,
                tracerWrapper
        );
    }

    @Test
    void givenAnValidAttempt_whenCallPipeline_thenShouldReturnAssessmentAndRecord() {
        Attempt attempt = createValidAttempt();

        FraudStep denyStep = new FraudStep() {
            @Override
            public String name() {
                return "test-deny";
            }

            @Override
            public void run(FraudCase fraudCase) {
                fraudCase.decide(Verdict.DENY, 1.0);
            }

            @Override
            public void onFailure(
                    FraudCase fraudCase,
                    Exception error
            ) {
                fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);
            }
        };

        FraudPipeline pipeline = new FraudPipeline(applicationLogger, List.of(denyStep));

        DefaultAssessFraudUseCase useCase =
                new DefaultAssessFraudUseCase(
                        applicationLogger,
                        Map.of(Channel.PIX, pipeline),
                        statsRepository,
                        metrics,
                        tracerWrapper
                );

        Assessment result = useCase.execute(attempt);

        assertEquals(Verdict.DENY, result.verdict());
        verify(statsRepository).record(attempt, result);
    }

    @Test
    void givenAnInvalidNonSupportedChannel_whenCallPipeline_thenShouldReturnManualReview() {
        Attempt attempt = createValidAttempt();

        FraudPipeline pipeline = new FraudPipeline(applicationLogger, List.of());

        DefaultAssessFraudUseCase useCase =
                new DefaultAssessFraudUseCase(
                        applicationLogger,
                        Map.of(Channel.CARD, pipeline),
                        statsRepository,
                        metrics,
                        tracerWrapper
                );

        Assessment result = useCase.execute(attempt);

        assertEquals(Verdict.MANUAL_REVIEW, result.verdict());
        assertEquals(Double.NaN, result.score());
        assertEquals("UNSUPPORTED_CHANNEL", result.reasons().getFirst());
    }

    @Test
    void givenAnValidAttemptButThrowsOnRecord_whenCallPipeline_thenShouldReturn() {
        Attempt attempt = createValidAttempt();

        FraudStep approveStep = new FraudStep() {
            @Override
            public String name() {
                return "test-approve";
            }

            @Override
            public void run(FraudCase fraudCase) {
                fraudCase.decide(Verdict.APPROVE, 0.15);
            }

            @Override
            public void onFailure(
                    FraudCase fraudCase,
                    Exception error
            ) {
                fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);
            }
        };

        Mockito.doThrow(new RuntimeException("failed record")).when(statsRepository).record(Mockito.any(), Mockito.any());

        FraudPipeline pipeline = new FraudPipeline(applicationLogger, List.of(approveStep));

        DefaultAssessFraudUseCase useCase =
                new DefaultAssessFraudUseCase(
                        applicationLogger,
                        Map.of(Channel.PIX, pipeline),
                        statsRepository,
                        metrics,
                        tracerWrapper
                );

        Assessment result = useCase.execute(attempt);

        assertEquals(Verdict.APPROVE, result.verdict());
        verify(statsRepository).record(attempt, result);
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
