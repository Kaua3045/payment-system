package com.payment.system.domain.accounts;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.domain.Identifier;

public record AccountId(Ulid value) implements Identifier<Ulid> {

    private static final String SYSTEM_ULID = "01HZZZZZZZZZZZZZZZZZZZZZZZ";

    public AccountId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }

    public static AccountId system() {
        return new AccountId(Ulid.from(SYSTEM_ULID));
    }

    public boolean isSystem() {
        return Ulid.from(SYSTEM_ULID).equals(this.value());
    }
}
