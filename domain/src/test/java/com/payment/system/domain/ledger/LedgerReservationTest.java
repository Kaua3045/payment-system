package com.payment.system.domain.ledger;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.transactions.TransactionId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.handler.NotificationHandler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerReservationTest extends UnitTest {

    @Test
    void givenValidParameters_whenCreateLedgerReservation_thenReturnLedgerReservation() {
        final var aTransactionId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var anAmount = BigDecimal.valueOf(100);
        final var aExpiresAt = InstantUtils.now().plus(5, ChronoUnit.MINUTES);

        final var aLedgerReservation = LedgerReservation.newReservation(
                aTransactionId,
                aAccountId,
                anAmount,
                aExpiresAt
        );

        assertEquals(aTransactionId, aLedgerReservation.getTransactionId());
        assertEquals(aAccountId, aLedgerReservation.getAccountId());
        assertEquals(anAmount, aLedgerReservation.getAmount());
        assertEquals(aExpiresAt, aLedgerReservation.getExpiresAt());
        assertEquals(ReservationStatus.PENDING, aLedgerReservation.getStatus());
        assertDoesNotThrow(() -> aLedgerReservation.validate(NotificationHandler.create()));
    }

    @Test
    void givenValidParameters_whenCreateLedgerReservationWithCustomId_thenReturnLedgerReservation() {
        final var aTransactionId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var anAmount = BigDecimal.valueOf(100);
        final var aExpiresAt = InstantUtils.now().plus(5, ChronoUnit.MINUTES);
        final var aLedgerReservationId = new LedgerReservationId(IdentifierUtils.generateNewMonotonicULID());

        final var aLedgerReservation = LedgerReservation.with(
                aLedgerReservationId,
                0L,
                aTransactionId,
                aAccountId,
                anAmount,
                InstantUtils.now(),
                aExpiresAt,
                ReservationStatus.PENDING
        );

        assertEquals(aLedgerReservationId, aLedgerReservation.getId());
        assertEquals(aTransactionId, aLedgerReservation.getTransactionId());
        assertEquals(aAccountId, aLedgerReservation.getAccountId());
        assertEquals(anAmount, aLedgerReservation.getAmount());
        assertEquals(aExpiresAt, aLedgerReservation.getExpiresAt());
        assertEquals(ReservationStatus.PENDING, aLedgerReservation.getStatus());
        assertDoesNotThrow(() -> aLedgerReservation.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidLedgerReservation_whenConfirm_thenStatusShouldBeConfirmed() {
        final var aTransactionId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var anAmount = BigDecimal.valueOf(100);
        final var aExpiresAt = InstantUtils.now().plus(5, ChronoUnit.MINUTES);

        final var aLedgerReservation = LedgerReservation.newReservation(
                aTransactionId,
                aAccountId,
                anAmount,
                aExpiresAt
        );

        aLedgerReservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, aLedgerReservation.getStatus());
    }

    @Test
    void givenAValidLedgerReservation_whenCancel_thenStatusShouldBeCancelled() {
        final var aTransactionId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var anAmount = BigDecimal.valueOf(100);
        final var aExpiresAt = InstantUtils.now().plus(5, ChronoUnit.MINUTES);

        final var aLedgerReservation = LedgerReservation.newReservation(
                aTransactionId,
                aAccountId,
                anAmount,
                aExpiresAt
        );

        aLedgerReservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, aLedgerReservation.getStatus());
    }
}
