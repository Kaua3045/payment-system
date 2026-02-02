package com.payment.system.application.usecases.transactions.create;

import java.math.BigDecimal;

public record CreateTransactionCommand(
        String fromAccountId,
        String pixKey,
        BigDecimal amount,
        String idempotencyKey
) {

    public static CreateTransactionCommand with(
            final String aFromAccountId,
            final String aPixKey,
            final BigDecimal aAmount,
            final String aIdempotencyKey
    ) {
        return new CreateTransactionCommand(
                aFromAccountId,
                aPixKey,
                aAmount,
                aIdempotencyKey
        );
    }
}
