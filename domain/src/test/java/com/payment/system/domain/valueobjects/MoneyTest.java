package com.payment.system.domain.valueobjects;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest extends UnitTest {

    @Test
    void givenValidParams_whenCreateMoney_thenInstantiate() {
        final var actual = new Money(new BigDecimal("10.0"));

        assertEquals(10.00, actual.amount().doubleValue());
    }

    @Test
    void givenTwoMoney_whenAdd_thenReturnMoneyWithSum() {
        final var moneyOne = new Money(new BigDecimal("10.0"));
        final var moneyTwo = new Money(new BigDecimal("5.0"));

        final var actual = moneyOne.add(moneyTwo);

        assertEquals(15.00, actual.amount().doubleValue());
    }

    @Test
    void givenTwoMoney_whenSubtract_thenReturnMoneyWithDifference() {
        final var moneyOne = new Money(new BigDecimal("10.0"));
        final var moneyTwo = new Money(new BigDecimal("5.0"));

        final var actual = moneyOne.subtract(moneyTwo);

        assertEquals(5.00, actual.amount().doubleValue());
    }

    @Test
    void givenMoneyAndFactor_whenMultiply_thenReturnMoneyWithProduct() {
        final var money = new Money(new BigDecimal("10.0"));
        final var factor = 3;

        final var actual = money.multiply(factor);

        assertEquals(30.00, actual.amount().doubleValue());
    }

    @Test
    void givenTwoMoney_whenIsGreaterThanOrEqual_thenReturnTrue() {
        final var moneyOne = new Money(new BigDecimal("10.0"));
        final var moneyTwo = new Money(new BigDecimal("5.0"));

        final var actual = moneyOne.isGreaterThanOrEqual(moneyTwo);

        assertTrue(actual);
    }

    @Test
    void givenTwoMoney_whenIsLessThan_thenReturnTrue() {
        final var moneyOne = new Money(new BigDecimal("5.0"));
        final var moneyTwo = new Money(new BigDecimal("10.0"));

        final var actual = moneyOne.isLessThan(moneyTwo);

        assertTrue(actual);
    }

    @Test
    void givenNothing_whenCreateZeroMoney_thenReturnMoneyWithZeroAmount() {
        final var actual = Money.zero();

        assertEquals(0.00, actual.amount().doubleValue());
    }

    @Test
    void givenNullAmount_whenCreateMoney_thenShouldReceiveError() {
        final var aProperty = "amount";
        final var aMessage = "should not be null";

        final var aException = Assertions.assertThrows(ValidationException.class, () -> new Money(null));

        assertEquals(aMessage, aException.getErrors().getFirst().message());
        assertEquals(aProperty, aException.getErrors().getFirst().property());
    }

    @Test
    void givenNegativeAmount_whenCreateMoney_thenShouldReceiveError() {
        final var aProperty = "amount";
        final var aMessage = "should be greater than or equal to zero";

        final var aException = Assertions.assertThrows(ValidationException.class, () -> new Money(new BigDecimal("-1")));

        assertEquals(aMessage, aException.getErrors().getFirst().message());
        assertEquals(aProperty, aException.getErrors().getFirst().property());
    }
}
