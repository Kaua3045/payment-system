package com.payment.system.domain.pixkeys;

import com.payment.system.domain.Identifier;
import com.payment.system.domain.utils.ULID;

public record PixKeyId(ULID value) implements Identifier<ULID> {

    public PixKeyId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
