package com.payment.system.infrastructure.pixkeys.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyOutput;

public record CreatePixKeyResponse(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("account_id") String accountId
) {

    public static CreatePixKeyResponse from(final CreatePixKeyOutput aOutput) {
        return new CreatePixKeyResponse(
                aOutput.id(),
                aOutput.type(),
                aOutput.accountId()
        );
    }
}
