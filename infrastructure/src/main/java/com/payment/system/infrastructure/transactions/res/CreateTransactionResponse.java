package com.payment.system.infrastructure.transactions.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.transactions.create.CreateTransactionOutput;

public record CreateTransactionResponse(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("status") String status
) {

    public static CreateTransactionResponse from(final CreateTransactionOutput aOutput) {
        return new CreateTransactionResponse(
                aOutput.transactionId(),
                aOutput.status()
        );
    }
}
