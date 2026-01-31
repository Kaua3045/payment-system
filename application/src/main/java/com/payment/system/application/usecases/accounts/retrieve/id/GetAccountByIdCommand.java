package com.payment.system.application.usecases.accounts.retrieve.id;

public record GetAccountByIdCommand(String id) {

    public static GetAccountByIdCommand with(final String id) {
        return new GetAccountByIdCommand(id);
    }
}
