package com.payment.system.application.usecases.frauds.pipeline;

import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.frauds.Attempt;

import java.util.List;
import java.util.Objects;

public final class FraudPipeline {

    private final ApplicationLogger logger;
    private final List<FraudStep> steps;

    public FraudPipeline(final ApplicationLogger logger, final List<FraudStep> steps) {
        this.logger = Objects.requireNonNull(logger);
        this.steps = List.copyOf(steps);
    }

    public FraudCase run(final Attempt attempt) {
        FraudCase fraudCase = new FraudCase(attempt);

        for (FraudStep step : steps) {
            if (fraudCase.isDecided()) {
                break;
            }

            try {
                step.run(fraudCase);
                fraudCase.path.add(step.name());
            } catch (final Exception ex) {
                // step failed mark ("!") in path, register cause and apply failure policy
                fraudCase.path.add(step.name() + "!");
                this.logger.warn("Step {} failed for attempt {}", step.name(), attempt.id(), ex);
                fraudCase.reasons.add("FALLBACK:" + step.name() + ":" + ex.getClass().getSimpleName());
                step.onFailure(fraudCase, ex);
            }
        }

        return fraudCase;
    }
}
