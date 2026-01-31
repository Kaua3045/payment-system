package com.payment.system.application.usecases.accounts.create;

import com.payment.system.domain.accounts.Account;

public record CreateAccountOutput(
        String id,
        String userId,
        String status
) {

    public static CreateAccountOutput from(final Account anAccount) {
        return new CreateAccountOutput(
                anAccount.getId().value().toString(),
                anAccount.getUserId(),
                anAccount.getStatus().name()
        );
    }
}
