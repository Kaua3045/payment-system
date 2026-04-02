package com.payment.system.infrastructure.pixkeys.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.pixkeys.retrieve.get.GetPixKeyByValueOutput;
import com.payment.system.domain.pixkeys.PixKey;

import java.time.Instant;

public record GetPixKeyByValueResponse(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("value") String value,
        @JsonProperty("account_id") String accountId,
        @JsonProperty("status") String status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("deleted_at") Instant deletedAt
) {

    public static GetPixKeyByValueResponse from(final GetPixKeyByValueOutput aOutput) {
        return new GetPixKeyByValueResponse(
                aOutput.id(),
                aOutput.type(),
                aOutput.value(),
                aOutput.accountId(),
                aOutput.status(),
                aOutput.createdAt(),
                aOutput.updatedAt(),
                aOutput.deletedAt()
        );
    }
}
