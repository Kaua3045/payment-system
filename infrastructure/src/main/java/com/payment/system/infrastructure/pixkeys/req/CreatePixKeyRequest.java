package com.payment.system.infrastructure.pixkeys.req;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreatePixKeyRequest(
        @JsonProperty("type") String type,
        @JsonProperty("value") String value,
        @JsonProperty("account_id") String accountId
) {
}
