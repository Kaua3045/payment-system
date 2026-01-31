package com.payment.system.domain.valueobjects;

import com.payment.system.domain.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements ValueObject {

    public Money {
        this.assertArgumentNotNull(amount, "amount", "should not be null");
        this.assertArgumentTrue(amount.compareTo(BigDecimal.ZERO) >= 0, "amount", "should be greater than or equal to zero");
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

    public boolean isLessThan(final Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public BigDecimal amount() {
        return this.amount.setScale(2, RoundingMode.HALF_UP);
    }
}
