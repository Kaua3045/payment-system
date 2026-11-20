package com.payment.system.application.usecases.frauds.pipeline.steps;

import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudPipeline;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.RequestContext;
import com.payment.system.domain.frauds.Stats;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.features.FeatureExtractor;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnrichStepTest extends UnitTest {

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
                NOW,
                new RequestContext("123.456.789.012")
        );
    }

    @Test
    void nameShouldReturnEnrich() {
        StatsRepository repository = mock(StatsRepository.class);
        FeatureExtractor extractor = new FeatureExtractor();

        EnrichStep step = new EnrichStep(repository, extractor);

        assertEquals("enrich", step.name());
    }

    @Test
    void givenSuccessfulEnrichment_whenRun_shouldSetStatsAndFeatures()
            throws Exception {
        StatsRepository repository = mock(StatsRepository.class);
        FeatureExtractor extractor = new FeatureExtractor();
        Stats stats = Stats.empty();

        when(repository.load(any(Attempt.class))).thenReturn(stats);

        FraudCase fraudCase = createFraudCase(attempt());
        EnrichStep step = new EnrichStep(repository, extractor);

        step.run(fraudCase);

        assertSame(stats, fraudCase.getStats());
        assertNotNull(fraudCase.getFeatures());
        assertFalse(fraudCase.isDegraded());

        verify(repository).load(fraudCase.getAttempt());
    }

    @Test
    void givenStatsLoadingFailure_whenOnFailure_shouldUseEmptyStats()
            throws Exception {
        StatsRepository repository = mock(StatsRepository.class);
        FeatureExtractor extractor = new FeatureExtractor();

        when(repository.load(any(Attempt.class)))
                .thenThrow(new RuntimeException("stats unavailable"));

        FraudCase fraudCase = createFraudCase(attempt());
        EnrichStep step = new EnrichStep(repository, extractor);

        assertThrows(RuntimeException.class, () -> step.run(fraudCase));

        step.onFailure(fraudCase, new RuntimeException("stats unavailable"));

        assertEquals(Stats.empty(), fraudCase.getStats());
        assertNotNull(fraudCase.getFeatures());
        assertTrue(fraudCase.isDegraded());
    }

    @Test
    void givenFailure_whenOnFailure_shouldMarkCaseAsDegraded() {
        StatsRepository repository = mock(StatsRepository.class);
        FeatureExtractor extractor = new FeatureExtractor();

        FraudCase fraudCase = createFraudCase(attempt());
        EnrichStep step = new EnrichStep(repository, extractor);

        step.onFailure(fraudCase, new RuntimeException("stats unavailable"));

        assertEquals(Stats.empty(), fraudCase.getStats());
        assertNotNull(fraudCase.getFeatures());
        assertTrue(fraudCase.isDegraded());
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
}
