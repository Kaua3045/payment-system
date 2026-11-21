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
                logger.warn("Unsupported channel={} for attempt={}", attempt.channel().name(), attempt.id());
                return new Assessment(Verdict.MANUAL_REVIEW, Double.NaN, List.of("UNSUPPORTED_CHANNEL"));
            }

            // TODO precisa adicionar o step de location, que vai pegar a location do ip e adicionar no attempt, e ai o enrich step vai pegar a location e adicionar no stats, e ai o feature extractor vai pegar a location e adicionar na feature vector
            long started = System.nanoTime();
            final var fraudCase = pipeline.run(attempt);
            final var result = fraudCase.toAssessment();
            recordQuietly(attempt, result); // always, even when a strict rule brought things to an early end
            logDecision(attempt, result, fraudCase.path(), (System.nanoTime() - started) / 1_000);

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
            logger.error("Failed to record fraud assessment for attempt={}", attempt.id(), ex);
        }
    }

    /**
     * Uma linha por decisão, e o NÍVEL decide o volume: degradada = WARN, REVIEW/DENY = INFO, APPROVE = DEBUG (desligado em
     * produção). Sem amostragem em código: quem controla é o nível do logger "frauds.usecase.AssessFraudUseCase".
     * Nada de dado pessoal: só ids opacos (attempt), o canal, o veredito e os motivos.
     */
    private void logDecision(final Attempt attempt, final Assessment result, final String path, final long micros) {
        if (result.degraded()) {
            logger.warn("Decision for attempt={} channel={} verdict={} score={} degraded=true path={} reasons={} time={}us",
                    attempt.id(), attempt.channel(), result.verdict(), score(result.score()), path, result.reasons(), micros);
        } else if (result.verdict() != Verdict.APPROVE) {
            logger.info("Decision for attempt={} channel={} verdict={} score={} degraded=false path={} reasons={} time={}us",
                    attempt.id(), attempt.channel(), result.verdict(), score(result.score()), path, result.reasons(), micros);
        }
        // TODO add  is debug enabled
    }

    private static String score(double s) {
        return Double.isNaN(s) ? "na" : String.format(java.util.Locale.ROOT, "%.4f", s);
    }
}
