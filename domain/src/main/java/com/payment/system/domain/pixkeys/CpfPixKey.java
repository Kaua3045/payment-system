package com.payment.system.domain.pixkeys;

import com.payment.system.domain.utils.CpfUtils;

public record CpfPixKey(String value) implements PixKeyValue {

    public CpfPixKey {
        this.assertArgumentNotEmpty(value, "value", "CPF value must not be empty");
        this.assertArgumentTrue(CpfUtils.validateCpf(value), "value", "CPF value is invalid");
        value = CpfUtils.cleanCpf(value);
    }

    @Override
    public PixKeyType type() {
        return PixKeyType.CPF;
    }
}
