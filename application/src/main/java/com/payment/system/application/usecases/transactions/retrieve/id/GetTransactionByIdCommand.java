package com.payment.system.application.usecases.transactions.retrieve.id;

public record GetTransactionByIdCommand(
        String transactionId,
        String accountId
) {

    public static GetTransactionByIdCommand with(
            final String aTransactionId,
            final String aAccountId
    ) {
        return new GetTransactionByIdCommand(aTransactionId, aAccountId);
    }
}
