package com.payment.system.application.usecases.transactions.create;

import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;

public record CreateTransactionCommand(
        String fromAccountId,
        String pixKey,
        String pixKeyType,
        Money amount,
        String idempotencyKey
) {

    public static CreateTransactionCommand with(
            final String aFromAccountId,
            final String aPixKey,
            final String aPixKeyType,
            final BigDecimal aAmount,
            final String aIdempotencyKey
    ) {
        return new CreateTransactionCommand(
                aFromAccountId,
                aPixKey,
                aPixKeyType,
                Money.of(aAmount),
                aIdempotencyKey
        );
    }
}
