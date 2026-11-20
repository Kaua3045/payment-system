package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapshotNotExistsExceptionTest extends UnitTest {

    @Test
    void givenAnAccountId_whenCallsConstructor_thenShouldCreateSnapshotNotExistsException() {
        final var accountId = "account-123";
        final var exception = new SnapshotNotExistsException(accountId);

        assertEquals("Snapshot not exists for accountId account-123", exception.getMessage());
        assertEquals("snapshot_not_exists", exception.getErrorCode());
    }
}
