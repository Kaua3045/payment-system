package com.payment.system.infrastructure.transactions.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @JsonProperty("from_account_id") String fromAccountId,
        @JsonProperty("pix_key") String pixKey,
        @JsonProperty("pix_key_type") String pixKeyType,
        @JsonProperty("amount") BigDecimal amount
) {
}
