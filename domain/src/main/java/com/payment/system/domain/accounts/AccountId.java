package com.payment.system.domain.accounts;

import com.payment.system.domain.Identifier;
import com.payment.system.domain.utils.ULID;

public record AccountId(ULID value) implements Identifier<ULID> {

    public AccountId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
