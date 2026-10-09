package com.payment.system.domain.valueobjects;

import com.payment.system.domain.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements ValueObject {

    public Money {
        this.assertArgumentNotNull(amount, "amount", "should not be null");
        this.assertArgumentTrue(amount.scale() <= 2, "amount", "should not have more than 2 decimal places");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(final Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(final Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(final int factor) {
        return new Money(this.amount.multiply(new BigDecimal(factor)));
    }

    public boolean isGreaterThanOrEqual(final Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isGreaterThan(final Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(final Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isLessThanOrEqual(final Money other) {
        return this.amount.compareTo(other.amount) <= 0;
    }

    public long minorUnits() {
        return amount.movePointRight(2).longValueExact();
    }

    public Money negate() {
        return new Money(this.amount.negate());
    }
}
