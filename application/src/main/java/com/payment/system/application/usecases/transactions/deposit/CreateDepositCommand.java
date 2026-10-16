package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;

public record CreateDepositCommand(
        String pixKey,
        String pixKeyType,
        String source,
        Money amount,
        String idempotencyKey
) {

    public static CreateDepositCommand with(
            final String aPixKey,
            final String aPixKeyType,
            final String aSource,
            final BigDecimal aAmount,
            final String aIdempotencyKey
    ) {
        return new CreateDepositCommand(
                aPixKey,
                aPixKeyType,
                aSource,
                Money.of(aAmount),
                aIdempotencyKey
        );
    }
}
