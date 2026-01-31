package com.payment.system.infrastructure.accounts.req;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateAccountRequest(
        @JsonProperty("user_id") String userId
) {
}
