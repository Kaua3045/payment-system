package com.payment.system.domain.transactions;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.handler.NotificationHandler;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class TransactionTest extends UnitTest {

    @Test
    void givenAValidParams_whenCallsNewTransaction_thenInstantiateANewTransaction() {
        final var aFromAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aToAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aPixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var aAmount = new Money(new BigDecimal("150.00"));
        final var aType = TransactionType.TRANSFER;
        final var aSource = DepositSource.ATM;
        final var anIdempotencyKey = "unique-key-123";

        final var aTransaction = Transaction.newTransaction(
                aFromAccountId,
                aToAccountId,
                aPixKeyId,
                aAmount,
                aType,
                aSource,
                anIdempotencyKey
        );

        Assertions.assertNotNull(aTransaction);
        Assertions.assertEquals(aFromAccountId, aTransaction.getFromAccountId());
        Assertions.assertEquals(aToAccountId, aTransaction.getToAccountId());
        Assertions.assertEquals(aPixKeyId, aTransaction.getPixKeyId());
        Assertions.assertEquals(aAmount, aTransaction.getAmount());
        Assertions.assertEquals(TransactionStatus.PENDING, aTransaction.getStatus());
        Assertions.assertEquals(aType, aTransaction.getType());
        Assertions.assertEquals(aSource, aTransaction.getSource());
        Assertions.assertEquals(anIdempotencyKey, aTransaction.getIdempotencyKey());
        Assertions.assertNotNull(aTransaction.getCreatedAt());
        Assertions.assertNotNull(aTransaction.getUpdatedAt());
        Assertions.assertTrue(aTransaction.getFailureReason().isEmpty());
        Assertions.assertDoesNotThrow(() -> aTransaction.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidValues_whenCallsWithTransaction_thenReturnANewTransactionWithUpdatedValues() {
        final var aId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var aFromAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aToAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aPixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var aAmount = new Money(new BigDecimal("150.00"));
        final var anIdempotencyKey = "unique-key-123";
        final var aStatus = TransactionStatus.PENDING;
        final var aType = TransactionType.TRANSFER;
        final var aSource = DepositSource.CASH;
        final var aNow = InstantUtils.now();

        final var aTransaction = Transaction.with(
                aId,
                1L,
                aFromAccountId,
                aToAccountId,
                aPixKeyId,
                aAmount,
                aStatus,
                aType,
                aSource,
                anIdempotencyKey,
                null,
                aNow,
                aNow
        );

        Assertions.assertNotNull(aTransaction);
        Assertions.assertEquals(aId, aTransaction.getId());
        Assertions.assertEquals(1L, aTransaction.getVersion());
        Assertions.assertEquals(aFromAccountId, aTransaction.getFromAccountId());
        Assertions.assertEquals(aToAccountId, aTransaction.getToAccountId());
        Assertions.assertEquals(aAmount, aTransaction.getAmount());
        Assertions.assertEquals(aStatus, aTransaction.getStatus());
        Assertions.assertEquals(aType, aTransaction.getType());
        Assertions.assertEquals(aSource, aTransaction.getSource());
        Assertions.assertEquals(anIdempotencyKey, aTransaction.getIdempotencyKey());
        Assertions.assertEquals(aNow, aTransaction.getCreatedAt());
        Assertions.assertEquals(aNow, aTransaction.getUpdatedAt());
        Assertions.assertTrue(aTransaction.getFailureReason().isEmpty());
        Assertions.assertDoesNotThrow(() -> aTransaction.validate(NotificationHandler.create()));
    }

    @Test
    void givenAnInvalidStatusName_whenCallsTransactionStatusFrom_thenReturnEmpty() {
        final var invalidStatusName = "INVALID_STATUS";

        final var status = TransactionStatus.from(invalidStatusName);

        Assertions.assertTrue(status.isEmpty());
    }

    @Test
    void testCallToStringInTransaction() {
        final var aFromAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aToAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aPixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var aAmount = new Money(new BigDecimal("150.00"));
        final var aType = TransactionType.TRANSFER;
        final var aSource = DepositSource.CASH;
        final var anIdempotencyKey = "unique-key-123";

        final var aTransaction = Transaction.newTransaction(
                aFromAccountId,
                aToAccountId,
                aPixKeyId,
                aAmount,
                aType,
                aSource,
                anIdempotencyKey
        );

        Assertions.assertNotNull(aTransaction.toString());
    }

    @Test
    void givenAValidTransaction_whenCallsComplete_thenReturnUpdatedTransaction() {
        final var aFromAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aToAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aPixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var aAmount = new Money(new BigDecimal("150.00"));
        final var aType = TransactionType.TRANSFER;
        final var aSource = DepositSource.CASH;
        final var anIdempotencyKey = "unique-key-123";

        final var aTransaction = Transaction.newTransaction(
                aFromAccountId,
                aToAccountId,
                aPixKeyId,
                aAmount,
                aType,
                aSource,
                anIdempotencyKey
        );

        aTransaction.complete();

        Assertions.assertEquals(TransactionStatus.COMPLETED, aTransaction.getStatus());
    }

    @Test
    void givenAnInvalidTypeName_whenCallsTransactionTypeFrom_thenReturnEmpty() {
        final var invalidTypeName = "INVALID_TYPE";

        final var type = TransactionType.from(invalidTypeName);

        Assertions.assertTrue(type.isEmpty());
    }

    @Test
    void givenAValidTransaction_whenCallsFail_thenReturnUpdatedTransaction() {
        final var aFromAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aToAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aPixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var aAmount = new Money(new BigDecimal("150.00"));
        final var aType = TransactionType.TRANSFER;
        final var aSource = DepositSource.CASH;
        final var anIdempotencyKey = "unique-key-123";

        final var aTransaction = Transaction.newTransaction(
                aFromAccountId,
                aToAccountId,
                aPixKeyId,
                aAmount,
                aType,
                aSource,
                anIdempotencyKey
        );

        aTransaction.fail("Error on process transaction");

        Assertions.assertEquals(TransactionStatus.FAILED, aTransaction.getStatus());
    }

    @Test
    void givenAnInvalidDepositSourceName_whenCallsDepositSourceFrom_thenReturnEmpty() {
        final var invalidDepositSourceName = "INVALID_Source";

        final var source = DepositSource.from(invalidDepositSourceName);

        Assertions.assertTrue(source.isEmpty());
    }
}
