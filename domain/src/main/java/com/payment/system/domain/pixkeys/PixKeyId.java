package com.payment.system.domain.pixkeys;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.domain.Identifier;
import com.payment.system.domain.utils.ULID;

public record PixKeyId(Ulid value) implements Identifier<Ulid> {

    public PixKeyId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
