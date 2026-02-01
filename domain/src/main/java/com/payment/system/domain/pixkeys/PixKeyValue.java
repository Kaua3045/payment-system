package com.payment.system.domain.pixkeys;

import com.payment.system.domain.ValueObject;

public interface PixKeyValue extends ValueObject {
    PixKeyType type();
    String value();
}
