package com.payment.system.application.usecases.accounts.close;

public record CloseAccountCommand(
        String accountId
) {

    public static CloseAccountCommand with(
            final String aAccountId
    ) {
        return new CloseAccountCommand(aAccountId);
    }
}
