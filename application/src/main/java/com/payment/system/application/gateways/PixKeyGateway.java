package com.payment.system.application.gateways;

import com.payment.system.domain.pixkeys.PixKeyId;

import java.time.Instant;

public interface PixKeyGateway {

    PixKeyActiveResponse pixKeyOfActiveByValue(final String aType, final String aValue);

    record PixKeyActiveResponse(
            boolean active,
            PixKeyId pixKeyId,
            String accountId,
            Instant pixKeyCreatedAt
    ) {
    }
}
