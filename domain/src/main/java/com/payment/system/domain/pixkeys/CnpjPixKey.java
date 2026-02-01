package com.payment.system.domain.pixkeys;

import com.payment.system.domain.utils.CnpjUtils;

public record CnpjPixKey(String value) implements PixKeyValue {

    public CnpjPixKey {
        this.assertArgumentNotEmpty(value, "value", "CNPJ value must not be empty");
        this.assertArgumentTrue(CnpjUtils.validateCnpj(value), "value", "CNPJ value is invalid");
        value = CnpjUtils.cleanCnpj(value);
    }

    @Override
    public PixKeyType type() {
        return PixKeyType.CNPJ;
    }
}
