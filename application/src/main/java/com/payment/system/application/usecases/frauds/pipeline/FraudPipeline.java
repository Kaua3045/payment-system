package com.payment.system.application.usecases.frauds.pipeline;

import com.payment.system.domain.frauds.Attempt;

import java.util.List;

public final class FraudPipeline {

    private final List<FraudStep> steps;

    public FraudPipeline(final List<FraudStep> steps) {
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
                fraudCase.reasons.add("FALLBACK:" + step.name() + ":" + ex.getClass().getSimpleName());
                step.onFailure(fraudCase, ex);
            }
        }

        return fraudCase;
    }
}
