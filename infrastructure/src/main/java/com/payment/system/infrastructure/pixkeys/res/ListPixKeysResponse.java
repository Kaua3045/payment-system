package com.payment.system.infrastructure.pixkeys.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.pixkeys.retrieve.list.ListPixKeysOutput;

import java.time.Instant;

public record ListPixKeysResponse(
        @JsonProperty("pix_key_id") String pixKeyId,
        @JsonProperty("account_id") String accountId,
        @JsonProperty("value") String value,
        @JsonProperty("type") String type,
        @JsonProperty("status") String status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("deleted_at") Instant deletedAt
) {

    public static ListPixKeysResponse from(final ListPixKeysOutput aOutput) {
        return new ListPixKeysResponse(
                aOutput.pixKeyId(),
                aOutput.accountId(),
                aOutput.value(),
                aOutput.type(),
                aOutput.status(),
                aOutput.createdAt(),
                aOutput.updatedAt(),
                aOutput.deletedAt()
        );
    }
}
