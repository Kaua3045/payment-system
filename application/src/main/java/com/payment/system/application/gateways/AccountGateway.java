package com.payment.system.application.gateways;

import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;

public interface AccountGateway {

    AccountResponse accountOfId(final String accountId);

    record AccountResponse(
            AccountStatus status,
            AccountId accountId
    ) {
    }
}
