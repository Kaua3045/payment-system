package com.payment.system.infrastructure.accounts.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.accounts.create.CreateAccountOutput;

public record CreateAccountResponse(
        @JsonProperty("id") String id,
        @JsonProperty("user_id") String userId,
        @JsonProperty("status") String status
) {

    public static CreateAccountResponse from(final CreateAccountOutput aOutput) {
        return new CreateAccountResponse(
                aOutput.id(),
                aOutput.userId(),
                aOutput.status()
        );
    }
}
