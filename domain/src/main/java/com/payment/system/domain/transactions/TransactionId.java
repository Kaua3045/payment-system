package com.payment.system.domain.transactions;

import com.payment.system.domain.Identifier;
import com.payment.system.domain.utils.ULID;

public record TransactionId(ULID value) implements Identifier<ULID> {

    public TransactionId {
        this.assertArgumentNotNull(value, "id", "should not be null");
    }
}
