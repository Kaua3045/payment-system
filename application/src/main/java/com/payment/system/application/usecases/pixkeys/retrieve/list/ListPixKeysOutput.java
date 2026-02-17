package com.payment.system.application.usecases.pixkeys.retrieve.list;

import com.payment.system.application.usecases.pixkeys.presentations.PixKeyFormatter;
import com.payment.system.domain.pixkeys.PixKey;

import java.time.Instant;

public record ListPixKeysOutput(
        String pixKeyId,
        String accountId,
        String value,
        String type,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {

    public static ListPixKeysOutput from(final PixKey aKey) {
        return new ListPixKeysOutput(
                aKey.getId().value().toString(),
                aKey.getAccountId().value().toString(),
                PixKeyFormatter.format(aKey.getKey().value(), aKey.getKey().type()),
                aKey.getKey().type().name(),
                aKey.getStatus().name(),
                aKey.getCreatedAt(),
                aKey.getUpdatedAt(),
                aKey.getDeletedAt().orElse(null)
        );
    }
}
