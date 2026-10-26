package com.payment.system.infrastructure.configurations;

import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.usecases.frauds.assess.AssessFraudUseCase;
import com.payment.system.application.usecases.frauds.assess.DefaultAssessFraudUseCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudPipeline;
import com.payment.system.application.usecases.frauds.pipeline.steps.EnrichStep;
import com.payment.system.application.usecases.frauds.pipeline.steps.RulesStep;
import com.payment.system.application.usecases.frauds.pipeline.steps.ScoreStep;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.domain.frauds.Channel;
import com.payment.system.domain.frauds.features.FeatureExtractor;
import com.payment.system.domain.frauds.features.FeatureVector;
import com.payment.system.domain.frauds.model.TreeModel;
import com.payment.system.domain.frauds.model.TreeModelParser;
import com.payment.system.domain.frauds.rules.BlockList;
import com.payment.system.domain.frauds.rules.Rules;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.configurations.properties.FraudSettings;
import com.payment.system.infrastructure.wrapper.Slf4jApplicationLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FraudModule {

    private final AssessFraudUseCase useCase;
    private final BlockList blockList;

    public FraudModule(final AssessFraudUseCase useCase, final BlockList blockList) {
        this.useCase = Objects.requireNonNull(useCase);
        this.blockList = Objects.requireNonNull(blockList);
    }

    public static FraudModule create(
            final FraudSettings settings,
            final BlockList blockList,
            final StatsRepository stats,
            final Metrics metrics,
            final TracerWrapper tracer
    ) throws IOException {
        try {
            final var pixPipeline = pipeline(TreeModelParser.parse(settings.getModelPath()), settings, stats, blockList);
            Map<Channel, FraudPipeline> pipelines = Map.of(Channel.PIX, pixPipeline);
            return new FraudModule(new DefaultAssessFraudUseCase(
                    new Slf4jApplicationLogger(AssessFraudUseCase.class), pipelines, stats, metrics, tracer
            ), blockList);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private static FraudPipeline pipeline(TreeModel model, FraudSettings settings, StatsRepository stats, BlockList blockList) {
        return new FraudPipeline(List.of(
                new EnrichStep(stats, new FeatureExtractor()),
                new RulesStep(rules(settings, blockList)),
                new ScoreStep(model, settings.getReviewAt(), settings.getDenyAt(), settings.getFailOpenBelow())
        ));
    }

    private static List<Rules.Rule> rules(final FraudSettings settings, final BlockList blockList) {
        return List.of(
                Rules.blocklist(blockList), // strong
                Rules.amountLimit(Money.of(settings.getAmountLimit())), // strong
                Rules.nightAmountLimit(Money.of(settings.getNightLimit()), Set.of(Channel.PIX)), // strong
                Rules.featureAtLeast(FeatureVector.COUNT_1M_LOG, Math.log1p(settings.getMaxPerMinute()),
                        "VELOCITY_1M", settings.getVelocityBoost()), // small
                Rules.featureAtLeast(FeatureVector.SPEED_SCALED, settings.getMaxSpeedKmh() / 1000.0,
                        "IMPOSSIBLE_TRAVEL", settings.getTravelBoost()) // small
        );
    }

    public AssessFraudUseCase getUseCase() {
        return useCase;
    }
}
