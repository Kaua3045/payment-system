package com.payment.system.domain.transactions;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.domain.Identifier;
import com.payment.system.domain.utils.ULID;

public record TransactionId(Ulid value) implements Identifier<Ulid> {

    public TransactionId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
