package com.payment.system.application.usecases.transactions.deposit;

import java.math.BigDecimal;

public record CreateDepositCommand(
        String pixKey,
        String pixKeyType,
        String source,
        BigDecimal amount,
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
                aAmount,
                aIdempotencyKey
        );
    }
}
