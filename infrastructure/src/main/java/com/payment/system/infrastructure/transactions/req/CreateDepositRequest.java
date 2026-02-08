package com.payment.system.infrastructure.transactions.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CreateDepositRequest(
        @JsonProperty("pix_key") String pixKey,
        @JsonProperty("pix_key_type") String pixKeyType,
        @JsonProperty("source") String source,
        @JsonProperty("amount") BigDecimal amount
) {
}
