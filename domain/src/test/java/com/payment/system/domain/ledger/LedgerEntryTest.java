package com.payment.system.domain.ledger;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.transactions.TransactionId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.validation.handler.NotificationHandler;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LedgerEntryTest extends UnitTest {

    @Test
    void givenAValidParams_whenCallsNewDebit_thenInstantiateIt() {
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aTransactionId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var anAmount = Money.of(BigDecimal.valueOf(100.0));

        final var aLedgerEntry = LedgerEntry.newDebit(aAccountId, aTransactionId, anAmount);

        assertNotNull(aLedgerEntry.getId());
        assertEquals(0L, aLedgerEntry.getVersion());
        assertEquals(aAccountId, aLedgerEntry.getAccountId());
        assertEquals(aTransactionId, aLedgerEntry.getTransactionId());
        assertEquals(anAmount.negate(), aLedgerEntry.getAmount());
        assertEquals(LedgerType.DEBIT, aLedgerEntry.getType());
        assertNotNull(aLedgerEntry.getCreatedAt());
        assertDoesNotThrow(() -> aLedgerEntry.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidParams_whenCallsNewCredit_thenInstantiateIt() {
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aTransactionId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var anAmount = Money.of(BigDecimal.valueOf(100.0));

        final var aLedgerEntry = LedgerEntry.newCredit(aAccountId, aTransactionId, anAmount);

        assertNotNull(aLedgerEntry.getId());
        assertEquals(0L, aLedgerEntry.getVersion());
        assertEquals(aAccountId, aLedgerEntry.getAccountId());
        assertEquals(aTransactionId, aLedgerEntry.getTransactionId());
        assertEquals(anAmount, aLedgerEntry.getAmount());
        assertEquals(LedgerType.CREDIT, aLedgerEntry.getType());
        assertNotNull(aLedgerEntry.getCreatedAt());
    }
}
