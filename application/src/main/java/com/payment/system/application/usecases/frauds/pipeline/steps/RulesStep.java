package com.payment.system.application.usecases.frauds.pipeline.steps;

import com.payment.system.application.usecases.frauds.pipeline.FraudCase;
import com.payment.system.application.usecases.frauds.pipeline.FraudStep;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.rules.RuleResult;
import com.payment.system.domain.frauds.rules.Rules;

import java.util.List;
import java.util.Objects;

public final class RulesStep implements FraudStep {

    private final List<Rules.Rule> rules;

    public RulesStep(final List<Rules.Rule> rules) {
        this.rules = Objects.requireNonNull(List.copyOf(rules));
    }

    @Override
    public String name() {
        return "rules";
    }

    @Override
    public void run(final FraudCase fraudCase) throws Exception {
        for (Rules.Rule rule : rules) {
            final var result = rule.check(fraudCase.getAttempt(), fraudCase.getFeatures());
            if (result instanceof RuleResult.Deny deny) {
                fraudCase.addReason(deny.reason());
                fraudCase.decide(Verdict.DENY, 1.0);
                return;
            }
            if (result instanceof RuleResult.Boost boost) {
                fraudCase.boostLogitIncrement(boost.logit());
                fraudCase.addReason(boost.reason());
            }
        }
    }

    @Override
    public void onFailure(final FraudCase fraudCase, final Exception error) {
        fraudCase.decide(Verdict.MANUAL_REVIEW, Double.NaN);
        fraudCase.setDegraded(true);
    }
}
