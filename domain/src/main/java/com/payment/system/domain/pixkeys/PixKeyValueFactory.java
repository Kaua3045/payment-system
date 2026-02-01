package com.payment.system.domain.pixkeys;

import com.payment.system.domain.exceptions.DomainException;

public class PixKeyValueFactory {

    public PixKeyValueFactory() {
    }

    public PixKeyValue create(final PixKeyType aType, final String aRaw) {
        return switch (aType) {
            case CPF -> new CpfPixKey(aRaw);
            case CNPJ -> new CnpjPixKey(aRaw);
            case EMAIL -> new EmailPixKey(aRaw);
            case RANDOM -> new RandomPixKey(aRaw);
            default -> throw DomainException.with("Pix key type %s not implemented".formatted(aType));
        };
    }
}
