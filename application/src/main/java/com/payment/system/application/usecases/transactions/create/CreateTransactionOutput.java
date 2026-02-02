package com.payment.system.application.usecases.transactions.create;

import com.payment.system.domain.transactions.Transaction;

public record CreateTransactionOutput(
        String transactionId,
        String status
) {

    public static CreateTransactionOutput from(final Transaction aTransaction) {
        return new CreateTransactionOutput(
                aTransaction.getId().value().toString(),
                aTransaction.getStatus().name()
        );
    }
}
