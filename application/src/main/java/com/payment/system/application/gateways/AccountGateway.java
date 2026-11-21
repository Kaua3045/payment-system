package com.payment.system.application.gateways;

import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;

import java.time.Instant;

public interface AccountGateway {

    AccountResponse accountOfId(final String accountId);

    record AccountResponse(
            AccountStatus status,
            AccountId accountId,
            Instant createdAt
    ) {
    }
}
