package com.payment.system.domain.pixkeys;

public record RandomPixKey(String value) implements PixKeyValue {

    @Override
    public PixKeyType type() {
        return PixKeyType.RANDOM;
    }
}
