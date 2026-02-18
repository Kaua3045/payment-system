package com.payment.system.application.usecases.pixkeys.presentations;

import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.utils.CnpjUtils;
import com.payment.system.domain.utils.CpfUtils;

public final class PixKeyFormatter {

    private PixKeyFormatter() {}

    public static String format(final String aValue, final PixKeyType aType) {
        return switch (aType) {
            case CPF -> CpfUtils.formatCpf(aValue);
            case CNPJ -> CnpjUtils.formatCnpj(aValue);
            default -> aValue;
        };
    }
}
