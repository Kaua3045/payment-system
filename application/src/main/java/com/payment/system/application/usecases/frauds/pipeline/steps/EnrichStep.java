package com.payment.system.application.usecases.frauds.pipeline.steps;

import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.frauds.Stats;
import com.payment.system.domain.frauds.features.FeatureExtractor;

import java.util.Objects;

public final class EnrichStep implements FraudStep {

    private final StatsRepository statsRepository;
    private final FeatureExtractor featureExtractor;

    public EnrichStep(final StatsRepository statsRepository, final FeatureExtractor featureExtractor) {
        this.statsRepository = Objects.requireNonNull(statsRepository);
        this.featureExtractor = Objects.requireNonNull(featureExtractor);
    }

    @Override
    public String name() {
        return "enrich";
    }

    @Override
    public void run(final FraudCase fraudCase) throws Exception {
        final var stats = statsRepository.load(fraudCase.getAttempt());
        fraudCase.setStats(stats);
        fraudCase.setFeatures(featureExtractor.extract(fraudCase.getAttempt(), stats));
    }

    // Redis down strong rules security this and mark is degraded
    @Override
    public void onFailure(final FraudCase fraudCase, final Exception error) {
        fraudCase.setStats(Stats.empty());
        fraudCase.setFeatures(featureExtractor.extract(fraudCase.getAttempt(), fraudCase.getStats()));
        fraudCase.setDegraded(true);
    }
}
