package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.domain.transactions.Transaction;

public record CreateDepositOutput(
        String transactionId,
        String status,
        String type
) {

    public static CreateDepositOutput from(final Transaction aTransaction) {
        return new CreateDepositOutput(
                aTransaction.getId().value().toString(),
                aTransaction.getStatus().name(),
                aTransaction.getType().name()
        );
    }
}
