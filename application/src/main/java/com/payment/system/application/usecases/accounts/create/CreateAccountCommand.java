package com.payment.system.application.usecases.accounts.create;

public record CreateAccountCommand(
        String userId
) {

    public static CreateAccountCommand with(
            final String userId
    ) {
        return new CreateAccountCommand(userId);
    }
}
