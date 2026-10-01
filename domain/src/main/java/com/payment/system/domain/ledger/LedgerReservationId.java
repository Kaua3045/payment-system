package com.payment.system.domain.ledger;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.domain.Identifier;

public record LedgerReservationId(Ulid value) implements Identifier<Ulid> {
    public LedgerReservationId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
