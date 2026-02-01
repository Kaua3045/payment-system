package com.payment.system.application.usecases.pixkeys.create;

import com.payment.system.domain.pixkeys.PixKey;

public record CreatePixKeyOutput(
        String id,
        String type,
        String accountId
) {

    public static CreatePixKeyOutput from(final PixKey aPixKey) {
        return new CreatePixKeyOutput(
                aPixKey.getId().value().toString(),
                aPixKey.getType().name(),
                aPixKey.getAccountId().value().toString()
        );
    }
}
