package com.payment.system.application.usecases.transactions.retrieve.id;

import com.payment.system.domain.transactions.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record GetTransactionByIdOutput(
        String transactionId,
        long version,
        String fromAccountId,
        String toAccountId,
        String pixKeyId,
        BigDecimal amount,
        String status,
        String type,
        String idempotencyKey,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static GetTransactionByIdOutput from(final Transaction aTransaction) {
        return new GetTransactionByIdOutput(
                aTransaction.getId().value().toString(),
                aTransaction.getVersion(),
                aTransaction.getFromAccountId().value().toString(),
                aTransaction.getToAccountId().value().toString(),
                aTransaction.getPixKeyId().value().toString(),
                aTransaction.getAmount().amount(),
                aTransaction.getStatus().name(),
                aTransaction.getType().name(),
                aTransaction.getIdempotencyKey(),
                aTransaction.getFailureReason().orElse(null),
                aTransaction.getCreatedAt(),
                aTransaction.getUpdatedAt()
        );
    }
}
