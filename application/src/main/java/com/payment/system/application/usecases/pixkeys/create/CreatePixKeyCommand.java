package com.payment.system.application.usecases.pixkeys.create;

public record CreatePixKeyCommand(
        String type,
        String value,
        String accountId
) {

    public static CreatePixKeyCommand with(
            final String type,
            final String value,
            final String accountId
    ) {
        return new CreatePixKeyCommand(type, value, accountId);
    }
}
