package com.payment.system.infrastructure.gateways;

import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyStatus;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import org.springframework.stereotype.Component;

@Component
public class PixKeyGatewayImpl implements PixKeyGateway {

    private final PixKeyRepository pixKeyRepository;

    public PixKeyGatewayImpl(final PixKeyRepository pixKeyRepository) {
        this.pixKeyRepository = pixKeyRepository;
    }

    @Override
    public PixKeyActiveResponse pixKeyOfActiveByValue(final String aType, final String aValue) {
        final var aPixKeyType = PixKeyType.from(aType)
                .orElseThrow(() -> NotFoundException.with("PixKeyType %s not found".formatted(aType)));

        final var aKey = new PixKeyValueFactory().create(aPixKeyType, aValue);

        final var aPixKey = this.pixKeyRepository.pixKeyOfActiveByValue(aKey.value())
                .orElseThrow(NotFoundException.with(PixKey.class, "value", aValue));

        return new PixKeyActiveResponse(
                aPixKey.getStatus().equals(PixKeyStatus.ACTIVE),
                aPixKey.getId(),
                aPixKey.getAccountId().value().toString()
        );
    }
}
