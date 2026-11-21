package com.payment.system.domain.frauds.rules;

import com.payment.system.domain.validation.AssertionConcern;

public sealed interface RuleResult {

    Pass PASS = new Pass();

    record Pass() implements RuleResult {
    }

    record Deny(String reason) implements RuleResult {
    }

    // logit > 0 increment risk, < 0 decreases risk
    record Boost(String reason, double logit) implements RuleResult, AssertionConcern {
        public Boost {
            this.assertArgumentTrue(Double.isFinite(logit), "logit", "logit invalid");
        }
    }
}
