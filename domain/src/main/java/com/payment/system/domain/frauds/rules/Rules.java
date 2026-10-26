package com.payment.system.domain.frauds.rules;

import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Channel;
import com.payment.system.domain.frauds.features.FeatureVector;
import com.payment.system.domain.validation.Error;
import com.payment.system.domain.valueobjects.Money;

import java.util.Set;

public final class Rules {

    private Rules() {
    }

    @FunctionalInterface
    public interface Rule {
        RuleResult check(Attempt attempt, FeatureVector features);
    }

    public static Rule amountLimit(Money max) {
        requirePositive(max);
        return (attempt, features) -> attempt.amount().amount().compareTo(max.amount()) > 0 ? new RuleResult.Deny("AMOUNT_LIMIT") : RuleResult.PASS;
    }

    public static Rule blocklist(BlockList list) {
        return (attempt, features) -> {
            if (list.contains("acc:" + attempt.origin())) return new RuleResult.Deny("BLOCKLIST_ORIGIN");
            if (list.contains("acc:" + attempt.destination())) return new RuleResult.Deny("BLOCKLIST_DESTINATION");
            String instrument = attempt.details().blockKey();
            if (instrument != null && list.contains(instrument)) return new RuleResult.Deny("BLOCKLIST_INSTRUMENT");
            return RuleResult.PASS;
        };
    }

    // Night limit for specific channels
    public static Rule nightAmountLimit(Money maxNight, Set<Channel> channels) {
        requirePositive(maxNight);
        Set<Channel> applies = Set.copyOf(channels);
        return ((attempt, features) -> applies.contains(attempt.channel()) && features.get(FeatureVector.NIGHT) >= 1.0
                && attempt.amount().amount().compareTo(maxNight.amount()) > 0 ? new RuleResult.Deny("NIGHT_LIMIT") : RuleResult.PASS);
    }

    public static Rule featureAtLeast(int featureIndex, double threshold, String reason, double logit) {
        if (featureIndex < 0 || featureIndex >= FeatureVector.SIZE) {
            throw InternalErrorException.with("feature index invalid " + featureIndex);
        }
        RuleResult hit = new RuleResult.Boost(reason, logit);
        return (attempt, features) -> features.get(featureIndex) >= threshold ? hit : RuleResult.PASS;
    }

    private static void requirePositive(Money limit) {
        if (limit == null || limit.amount().signum() <= 0) {
            throw ValidationException.with(new Error("max", "should not be negative or null"));
        }
    }
}
