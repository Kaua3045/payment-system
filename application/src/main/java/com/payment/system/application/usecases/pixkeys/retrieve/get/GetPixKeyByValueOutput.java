package com.payment.system.application.usecases.pixkeys.retrieve.get;

import com.payment.system.domain.pixkeys.PixKey;

import java.time.Instant;

public record GetPixKeyByValueOutput(
        String id,
        String type,
        String value,
        String accountId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {

    public static GetPixKeyByValueOutput from(final PixKey aPixKey) {
        return new GetPixKeyByValueOutput(
                aPixKey.getId().value().toString(),
                aPixKey.getKey().type().name(),
                aPixKey.getKey().value(),
                aPixKey.getAccountId().value().toString(),
                aPixKey.getStatus().name(),
                aPixKey.getCreatedAt(),
                aPixKey.getUpdatedAt(),
                aPixKey.getDeletedAt().orElse(null)
        );
    }
}
