package com.payment.system.infrastructure.transactions.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositOutput;

public record CreateDepositResponse(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("status") String status,
        @JsonProperty("type") String type
) {

    public static CreateDepositResponse from(final CreateDepositOutput aOutput) {
        return new CreateDepositResponse(
                aOutput.transactionId(),
                aOutput.status(),
                aOutput.type()
        );
    }
}
