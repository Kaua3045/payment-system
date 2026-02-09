package com.payment.system.domain.accounts;

import com.payment.system.domain.Identifier;
import com.payment.system.domain.utils.ULID;

public record AccountId(ULID value) implements Identifier<ULID> {

    private static final String SYSTEM_ULID = "01HZZZZZZZZZZZZZZZZZZZZZZZ";

    public AccountId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }

    public static AccountId system() {
        return new AccountId(ULID.fromString(SYSTEM_ULID));
    }

    public boolean isSystem() {
        return ULID.fromString(SYSTEM_ULID).equals(this.value());
    }
}
