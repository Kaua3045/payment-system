package com.payment.system.infrastructure.accounts.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdOutput;

import java.math.BigDecimal;
import java.time.Instant;

public record GetAccountByIdResponse(
        @JsonProperty("id") String id,
        @JsonProperty("user_id") String userId,
        @JsonProperty("balance") BigDecimal balance,
        @JsonProperty("status") String status,
        @JsonProperty("version") long version,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("closed_at") Instant closedAt
) {

    public static GetAccountByIdResponse from(final GetAccountByIdOutput aOutput) {
        return new GetAccountByIdResponse(
                aOutput.id(),
                aOutput.userId(),
                aOutput.balance(),
                aOutput.status(),
                aOutput.version(),
                aOutput.createdAt(),
                aOutput.updatedAt(),
                aOutput.closedAt()
        );
    }
}
