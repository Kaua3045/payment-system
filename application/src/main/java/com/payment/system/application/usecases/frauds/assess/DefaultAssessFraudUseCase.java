package com.payment.system.application.usecases.frauds.assess;

import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.usecases.frauds.pipeline.FraudPipeline;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Channel;
import com.payment.system.domain.frauds.Verdict;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultAssessFraudUseCase extends AssessFraudUseCase {

    private final Map<Channel, FraudPipeline> pipelines;
    private final StatsRepository statsRepository;
    private final Metrics metrics;
    private final TracerWrapper tracer;

    public DefaultAssessFraudUseCase(
            final ApplicationLogger aLogger,
            final Map<Channel, FraudPipeline> pipelines,
            final StatsRepository statsRepository,
            final Metrics metrics,
            final TracerWrapper tracer
    ) {
        super(aLogger);
        this.pipelines = Map.copyOf(pipelines);
        this.statsRepository = Objects.requireNonNull(statsRepository);
        this.metrics = metrics;
        this.tracer = tracer;
    }

    @Override
    public Assessment execute(final Attempt attempt) {
        return this.tracer.traceWithReturn("fraud-assess", (ctx) -> {
            final var pipeline = pipelines.get(attempt.channel());
            if (pipeline == null) {
                ctx.setAttribute("fraud.unsupported_channel", attempt.channel().name());
                return new Assessment(Verdict.MANUAL_REVIEW, Double.NaN, List.of("UNSUPPORTED_CHANNEL"));
            }

            final var fraudCase = pipeline.run(attempt);
            final var result = fraudCase.toAssessment();
            recordQuietly(attempt, result); // always, even when a strict rule brought things to an early end

            ctx.setAttribute("fraud.channel", attempt.channel().name());
            ctx.setAttribute("fraud.path", fraudCase.path());
            ctx.setAttribute("fraud.verdict", result.verdict().name());
            ctx.setAttribute("fraud.score", result.score());
            ctx.setAttribute("fraud.degraded", result.degraded());
            ctx.setAttribute("fraud.reasons", result.reasons());
            return result;
        });
    }

    private void recordQuietly(final Attempt attempt, final Assessment result) {
        try {
            this.statsRepository.record(attempt, result);
        } catch (final RuntimeException ex) {
            // log and metrics fraud.record.failed
            this.metrics.incrementCounter("fraud.records", 1, Map.of(
                    "outcome", "error"
            ));
        }
    }
}
