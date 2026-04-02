package com.payment.system.application.usecases.pixkeys.retrieve.get;

public record GetPixKeyByValueCommand(String value) {

    public static GetPixKeyByValueCommand with(final String aValue) {
        return new GetPixKeyByValueCommand(aValue);
    }
}
