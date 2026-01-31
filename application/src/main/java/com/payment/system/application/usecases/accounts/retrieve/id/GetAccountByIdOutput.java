package com.payment.system.application.usecases.accounts.retrieve.id;

import com.payment.system.domain.accounts.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record GetAccountByIdOutput(
        String id,
        String userId,
        BigDecimal balance,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {

    public static GetAccountByIdOutput from(final Account anAccount) {
        return new GetAccountByIdOutput(
                anAccount.getId().value().toString(),
                anAccount.getUserId(),
                anAccount.getBalance().amount(),
                anAccount.getStatus().name(),
                anAccount.getVersion(),
                anAccount.getCreatedAt(),
                anAccount.getUpdatedAt(),
                anAccount.getClosedAt().orElse(null)
        );
    }
}
