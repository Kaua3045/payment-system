package com.payment.system.domain.frauds.rules;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Channel;
import com.payment.system.domain.frauds.features.FeatureVector;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RulesTest extends UnitTest {

    static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");
    static final FeatureVector DAY = FeatureVector.builder().build();
    static final FeatureVector NIGHT = FeatureVector.builder().set(FeatureVector.NIGHT, 1).build();

    static Attempt pix(String amount) {
        return Attempt.pix("id", "a", "b", "b@pix", NOW, Money.of(new BigDecimal(amount)), NOW, null, NOW);
    }

    @Test
    void blocklistContainsOrigin_shouldReturnDeny() {
        Rules.Rule rule = Rules.blocklist(Set.of("acc:a")::contains);
        assertEquals(new RuleResult.Deny("BLOCKLIST_ORIGIN"), rule.check(pix("1000"), DAY));
    }

    @Test
    void blocklistContainsDestination_shouldReturnDeny() {
        Rules.Rule rule = Rules.blocklist(Set.of("acc:b")::contains);
        assertEquals(new RuleResult.Deny("BLOCKLIST_DESTINATION"), rule.check(pix("1000"), DAY));
    }

    @Test
    void blocklistContainsInstrumentation_shouldReturnDeny() {
        Rules.Rule rule = Rules.blocklist(Set.of("key:b@pix")::contains);
        assertEquals(new RuleResult.Deny("BLOCKLIST_INSTRUMENT"), rule.check(pix("1000"), DAY));
    }

    @Test
    void blocklistNotContainsOrigin_shouldReturnPass() {
        Rules.Rule rule = Rules.blocklist(Set.of("acc:k")::contains);
        assertEquals(RuleResult.PASS, rule.check(pix("1000"), DAY));
    }

    @Test
    void amountLimitUsesCompareToNotEquals() {
        Rules.Rule rule = Rules.amountLimit(Money.of(new BigDecimal("5000")));
        assertEquals(RuleResult.PASS, rule.check(pix("5000.00"), DAY));
        assertEquals(new RuleResult.Deny("AMOUNT_LIMIT"), rule.check(pix("5000.01"), DAY));
    }

    @Test
    void nightLimitAppliesOnlyToTheConfiguredChannels() {
        Rules.Rule rule = Rules.nightAmountLimit(new Money(new BigDecimal("1000.00")), Set.of(Channel.PIX, Channel.TRANSFER));
        assertEquals(RuleResult.PASS, rule.check(pix("2000.00"), DAY));
        assertEquals(RuleResult.PASS, rule.check(pix("1000.00"), NIGHT));
        assertEquals(new RuleResult.Deny("NIGHT_LIMIT"), rule.check(pix("1000.01"), NIGHT));
    }

    @Test
    void limitsMustBePositive() {
        assertThrows(ValidationException.class, () -> Rules.amountLimit(Money.of(BigDecimal.ZERO)));
        assertThrows(ValidationException.class, () -> Rules.nightAmountLimit(null, Set.of(Channel.PIX)));
    }

    @Test
    void softRuleBoostsOnlyWhenTheFeatureReachesTheThreshold() {
        Rules.Rule rule = Rules.featureAtLeast(FeatureVector.COUNT_1M_LOG, 1.0, "VELOCITY_1M", 2.0);
        assertEquals(RuleResult.PASS, rule.check(pix("10.00"), DAY));
        FeatureVector burst = FeatureVector.builder().set(FeatureVector.COUNT_1M_LOG, 1.5).build();
        assertEquals(new RuleResult.Boost("VELOCITY_1M", 2.0), rule.check(pix("10.00"), burst));
    }

    @Test
    void givenNanLogit_whenCallsFeatureAtLeast_shouldThrowsException() {
        assertThrows(ValidationException.class, () -> Rules.featureAtLeast(FeatureVector.COUNT_1M_LOG, 1.0, "VELOCITY_1M", Double.NaN));
    }

    @Test
    void givenInfinityLogit_whenCallsFeatureAtLeast_shouldThrowsException() {
        assertThrows(ValidationException.class, () -> Rules.featureAtLeast(FeatureVector.COUNT_1M_LOG, 1.0, "VELOCITY_1M", Double.POSITIVE_INFINITY));
    }

    @Test
    void givenAnInvalidFeatureIndex_whenCallsFeatureAtLeast_shouldThrowsException() {
        assertThrows(InternalErrorException.class, () -> Rules.featureAtLeast(-1, 1.0, "VELOCITY_1M", Double.POSITIVE_INFINITY));
        assertThrows(InternalErrorException.class, () -> Rules.featureAtLeast(50, 1.0, "VELOCITY_1M", Double.POSITIVE_INFINITY));
    }
}
