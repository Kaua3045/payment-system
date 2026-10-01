package com.payment.system.application.gateways;

import com.payment.system.domain.pixkeys.PixKeyId;

public interface PixKeyGateway {

    PixKeyActiveResponse pixKeyOfActiveByValue(final String aType, final String aValue);

    record PixKeyActiveResponse(
            boolean active,
            PixKeyId pixKeyId,
            String accountId
    ) {
    }
}
