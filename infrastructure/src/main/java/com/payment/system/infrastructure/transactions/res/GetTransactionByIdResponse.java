package com.payment.system.infrastructure.transactions.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdOutput;

import java.math.BigDecimal;
import java.time.Instant;

public record GetTransactionByIdResponse(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("version") long version,
        @JsonProperty("from_account_id") String fromAccountId,
        @JsonProperty("to_account_id") String toAccountId,
        @JsonProperty("pix_key_id") String pixKeyId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("status") String status,
        @JsonProperty("type") String type,
        @JsonProperty("source") String source,
        @JsonProperty("idempotency_key") String idempotencyKey,
        @JsonProperty("failure_reason") String failureReason,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {

    public static GetTransactionByIdResponse from(final GetTransactionByIdOutput aOutput) {
        return new GetTransactionByIdResponse(
                aOutput.transactionId(),
                aOutput.version(),
                aOutput.fromAccountId(),
                aOutput.toAccountId(),
                aOutput.pixKeyId(),
                aOutput.amount(),
                aOutput.status(),
                aOutput.type(),
                aOutput.source(),
                aOutput.idempotencyKey(),
                aOutput.failureReason(),
                aOutput.createdAt(),
                aOutput.updatedAt()
        );
    }
}
