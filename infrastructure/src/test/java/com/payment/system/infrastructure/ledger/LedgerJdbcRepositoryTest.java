package com.payment.system.infrastructure.ledger;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.ledger.LedgerReservation;
import com.payment.system.domain.ledger.ReservationStatus;
import com.payment.system.domain.transactions.TransactionId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;

class LedgerJdbcRepositoryTest extends AbstractRepositoryTest {

    @Test
    void testAssertDependencies() {
        Assertions.assertNotNull(ledgerRepository());
    }

    @Test
    void givenAValidLedgerEntry_whenCallsSave_thenShouldPersistIt() {
        Assertions.assertEquals(0, countLedgerEntries());

        final var aLedgerEntry = LedgerEntry.newCredit(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                new BigDecimal(1000)
        );

        this.ledgerRepository().save(aLedgerEntry);

        Assertions.assertEquals(1, countLedgerEntries());
    }

    @Test
    void givenAnLedgersEntries_whenCallsSaveAll_thenShouldPersistThem() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));

        Assertions.assertEquals(0, countLedgerEntries());

        final var aLedgerEntry1 = LedgerEntry.newCredit(
                aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                new BigDecimal(1000)
        );

        final var aLedgerEntry2 = LedgerEntry.newDebit(
                aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                new BigDecimal(500)
        );

        this.ledgerRepository().saveAll(List.of(
                aLedgerEntry1,
                aLedgerEntry2
        ));

        Assertions.assertEquals(2, countLedgerEntries());
    }

    @Test
    void givenAnAccountId_whenCallsCalculateBalance_thenShouldReturnBalance() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));

        Assertions.assertEquals(0, countLedgerEntries());

        final var aLedgerEntry1 = LedgerEntry.newCredit(
                aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                new BigDecimal(1000)
        );

        final var aLedgerEntry2 = LedgerEntry.newDebit(
                aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                new BigDecimal(500)
        );

        this.ledgerRepository().saveAll(List.of(
                aLedgerEntry1,
                aLedgerEntry2
        ));

        Assertions.assertEquals(2, countLedgerEntries());

        final var balance = this.ledgerRepository().calculateBalance(aAccount.getId());

        Assertions.assertEquals(new BigDecimal(500).setScale(2, RoundingMode.HALF_UP), balance.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void givenAnAccountId_whenCallsCalculateBalanceWithNoEntries_thenShouldReturnZero() {
        Assertions.assertEquals(0, countLedgerEntries());

        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var balance = this.ledgerRepository().calculateBalance(aAccountId);

        Assertions.assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void givenAnReservation_whenCallsCreateReservation_thenShouldPersistIt() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));
        this.ledgerRepository().save(LedgerEntry.newCredit(aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()), new BigDecimal(1000)));

        Assertions.assertEquals(0, countLedgerReservations());

        final var aReservation = LedgerReservation.newReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                aAccount.getId(),
                new BigDecimal(1000),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        this.ledgerRepository().createReservation(aReservation);

        Assertions.assertEquals(1, countLedgerReservations());

        final var aSavedReservation = this.ledgerRepository().findReservationByTransactionId(aReservation.getTransactionId()).get();

        Assertions.assertEquals(aReservation.getId(), aSavedReservation.getId());
        Assertions.assertEquals(0, aSavedReservation.getVersion());
        Assertions.assertEquals(aReservation.getAccountId(), aSavedReservation.getAccountId());
        Assertions.assertEquals(aReservation.getTransactionId(), aSavedReservation.getTransactionId());
        Assertions.assertEquals(aReservation.getAmount().setScale(2, RoundingMode.HALF_UP), aSavedReservation.getAmount().setScale(2, RoundingMode.HALF_UP));
        Assertions.assertEquals(aReservation.getCreatedAt(), aSavedReservation.getCreatedAt());
        Assertions.assertEquals(aReservation.getExpiresAt(), aSavedReservation.getExpiresAt());
    }

    @Test
    void givenAnInsufficientFundsReservation_whenCallsCreateReservation_thenShouldThrowException() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));
        this.ledgerRepository().save(LedgerEntry.newCredit(aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()), new BigDecimal(1000)));

        Assertions.assertEquals(0, countLedgerReservations());

        final var aReservation = LedgerReservation.newReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                aAccount.getId(),
                new BigDecimal(1000),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        this.ledgerRepository().createReservation(aReservation);

        Assertions.assertEquals(1, countLedgerReservations());

        final var aSecondReservation = LedgerReservation.newReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                aReservation.getAccountId(),
                new BigDecimal(500),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        final var exception = Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> this.ledgerRepository().createReservation(aSecondReservation)
        );

        Assertions.assertEquals(
                "Insufficient funds",
                exception.getMessage()
        );
    }

    @Test
    void givenAnValidTransactionId_whenCallsConfirmReservation_thenShouldConfirmIt() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));
        this.ledgerRepository().save(LedgerEntry.newCredit(aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()), new BigDecimal(1000)));

        Assertions.assertEquals(0, countLedgerReservations());

        final var aReservation = LedgerReservation.newReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                aAccount.getId(),
                new BigDecimal(1000),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        this.ledgerRepository().createReservation(aReservation);

        Assertions.assertEquals(1, countLedgerReservations());

        this.ledgerRepository().confirmReservation(aReservation.getTransactionId());

        final var aConfirmedReservation = this.ledgerRepository().findReservationByTransactionId(aReservation.getTransactionId()).get();

        Assertions.assertEquals(ReservationStatus.CONFIRMED, aConfirmedReservation.getStatus());
    }

    @Test
    void givenAnValidTransactionId_whenCallsCancelReservation_thenShouldCancelIt() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));
        this.ledgerRepository().save(LedgerEntry.newCredit(aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()), new BigDecimal(1000)));

        Assertions.assertEquals(0, countLedgerReservations());

        final var aReservation = LedgerReservation.newReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                aAccount.getId(),
                new BigDecimal(1000),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        this.ledgerRepository().createReservation(aReservation);

        Assertions.assertEquals(1, countLedgerReservations());

        this.ledgerRepository().cancelReservation(aReservation.getTransactionId());

        final var aCancelledReservation = this.ledgerRepository().findReservationByTransactionId(aReservation.getTransactionId()).get();

        Assertions.assertEquals(ReservationStatus.CANCELLED, aCancelledReservation.getStatus());
    }

    @Test
    void givenAnValidTransactionId_whenCallsFindReservationByTransactionId_thenShouldReturnIt() {
        final var aAccount = this.accountRepository().save(Account.newAccount(IdentifierUtils.generateNewULID().toString()));
        this.ledgerRepository().save(LedgerEntry.newCredit(aAccount.getId(),
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()), new BigDecimal(1000)));

        Assertions.assertEquals(0, countLedgerReservations());

        final var aReservation = LedgerReservation.newReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                aAccount.getId(),
                new BigDecimal(1000),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        this.ledgerRepository().createReservation(aReservation);

        Assertions.assertEquals(1, countLedgerReservations());

        final var aFoundReservation = this.ledgerRepository().findReservationByTransactionId(aReservation.getTransactionId()).get();

        Assertions.assertEquals(aReservation.getId(), aFoundReservation.getId());
        Assertions.assertEquals(aReservation.getAccountId(), aFoundReservation.getAccountId());
        Assertions.assertEquals(aReservation.getTransactionId(), aFoundReservation.getTransactionId());
        Assertions.assertEquals(aReservation.getAmount().setScale(2, RoundingMode.HALF_UP), aFoundReservation.getAmount().setScale(2, RoundingMode.HALF_UP));
        Assertions.assertEquals(aReservation.getCreatedAt(), aFoundReservation.getCreatedAt());
        Assertions.assertEquals(aReservation.getExpiresAt(), aFoundReservation.getExpiresAt());
    }

    @Test
    void givenAnInvalidTransactionId_whenCallsFindReservationByTransactionId_thenShouldReturnEmpty() {
        Assertions.assertEquals(0, countLedgerReservations());

        final var aFoundReservation = this.ledgerRepository().findReservationByTransactionId(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID())
        );

        Assertions.assertTrue(aFoundReservation.isEmpty());
    }

    @Test
    void givenAnInvalidReservationReturnNull_whenCallsCancelReservation_thenShouldNotThrowException() {
        Assertions.assertEquals(0, countLedgerReservations());

        this.ledgerRepository().cancelReservation(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID())
        );
    }
}
