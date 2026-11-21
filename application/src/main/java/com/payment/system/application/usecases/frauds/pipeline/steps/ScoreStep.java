package com.payment.system.application.usecases.frauds.pipeline.steps;

import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.model.Sigmoid;
import com.payment.system.domain.frauds.model.TreeModel;

import java.math.BigDecimal;
import java.util.Objects;

public final class ScoreStep implements FraudStep {

    private final TreeModel model;
    private final double reviewAt;
    private final double denyAt;
    private final BigDecimal failOpenBelow;

    public ScoreStep(final TreeModel model, final double reviewAt, final double denyAt, final BigDecimal failOpenBelow) {
        this.model = Objects.requireNonNull(model);
        this.reviewAt = reviewAt;
        this.denyAt = denyAt;
        this.failOpenBelow = Objects.requireNonNull(failOpenBelow);
    }

    @Override
    public String name() {
        return "score";
    }

    @Override
    public void run(final FraudCase fraudCase) throws Exception {
        double raw = model.rawScore(fraudCase.getFeatures());

        if (Double.isNaN(raw) || Double.isInfinite(raw)) {
            throw InternalErrorException.with("model score invalid " + raw);
        }
        double score = Sigmoid.sigmoid(raw + fraudCase.getBoostLogit());
        fraudCase.decide(Verdict.of(score, reviewAt, denyAt), score);
    }

    @Override
    public void onFailure(final FraudCase fraudCase, final Exception error) {
        boolean small = fraudCase.getAttempt().amount().amount().compareTo(failOpenBelow) <= 0 && fraudCase.getBoostLogit() <= 0;
        fraudCase.decide(small ? Verdict.APPROVE : Verdict.MANUAL_REVIEW, Double.NaN);
        fraudCase.setDegraded(true);
    }
}
