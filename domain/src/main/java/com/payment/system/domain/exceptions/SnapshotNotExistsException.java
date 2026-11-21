package com.payment.system.domain.exceptions;

import java.util.ArrayList;

public class SnapshotNotExistsException extends DomainException {

    public SnapshotNotExistsException(final String accountId) {
        super("Snapshot not exists for accountId %s".formatted(accountId), new ArrayList<>(), "snapshot_not_exists");
    }
}
