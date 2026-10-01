package com.payment.system.domain.ledger;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.domain.Identifier;

public record LedgerEntryId(Ulid value) implements Identifier<Ulid> {
    public LedgerEntryId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
