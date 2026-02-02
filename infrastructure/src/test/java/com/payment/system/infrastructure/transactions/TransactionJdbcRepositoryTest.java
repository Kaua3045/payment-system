package com.payment.system.infrastructure.transactions;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.exceptions.ConflictException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class TransactionJdbcRepositoryTest extends AbstractRepositoryTest {

    @Test
    void testAssertDependencies() {
        Assertions.assertNotNull(transactionRepository());
    }

    @Test
    void givenAValidNewTransaction_whenCallsSave_thenShouldPersistIt() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                "1238712712678368126834"
        );

        final var aSavedTransaction = this.transactionRepository().save(aTransaction);

        Assertions.assertEquals(1, countTransactions());

        Assertions.assertEquals(aTransaction.getId(), aSavedTransaction.getId());
        Assertions.assertEquals(aTransaction.getFromAccountId(), aSavedTransaction.getFromAccountId());
        Assertions.assertEquals(aTransaction.getToAccountId(), aSavedTransaction.getToAccountId());
        Assertions.assertEquals(aTransaction.getPixKeyId(), aSavedTransaction.getPixKeyId());
        Assertions.assertEquals(aTransaction.getStatus(), aSavedTransaction.getStatus());
        Assertions.assertEquals(aTransaction.getType(), aSavedTransaction.getType());
        Assertions.assertEquals(aTransaction.getIdempotencyKey(), aSavedTransaction.getIdempotencyKey());
        Assertions.assertEquals(aTransaction.getCreatedAt(), aSavedTransaction.getCreatedAt());
        Assertions.assertEquals(aTransaction.getUpdatedAt(), aSavedTransaction.getUpdatedAt());
        Assertions.assertTrue(aSavedTransaction.getFailureReason().isEmpty());
    }

    @Test
    void givenAValidUpdatedTransaction_whenCallsSave_thenShouldUpdateIt() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                "1238712712678368126834"
        );

        final var aSavedTransaction = this.transactionRepository().save(aTransaction);

        aSavedTransaction.complete();

        this.transactionRepository().save(aSavedTransaction);

        Assertions.assertEquals(1, countTransactions());

        Assertions.assertEquals(aTransaction.getId(), aSavedTransaction.getId());
        Assertions.assertEquals(aTransaction.getFromAccountId(), aSavedTransaction.getFromAccountId());
        Assertions.assertEquals(aTransaction.getToAccountId(), aSavedTransaction.getToAccountId());
        Assertions.assertEquals(aTransaction.getPixKeyId(), aSavedTransaction.getPixKeyId());
        Assertions.assertEquals(aTransaction.getStatus(), aSavedTransaction.getStatus());
        Assertions.assertEquals(aTransaction.getType(), aSavedTransaction.getType());
        Assertions.assertEquals(aTransaction.getIdempotencyKey(), aSavedTransaction.getIdempotencyKey());
        Assertions.assertEquals(aTransaction.getCreatedAt(), aSavedTransaction.getCreatedAt());
        Assertions.assertEquals(aTransaction.getUpdatedAt(), aSavedTransaction.getUpdatedAt());
        Assertions.assertTrue(aSavedTransaction.getFailureReason().isEmpty());
    }

    @Test
    void givenAValidNotExistsIdempotencyKey_whenCallsExistsByIdempotencyKey_thenShouldReturnFalse() {
        Assertions.assertEquals(0, countTransactions());

        final var aIdempotencyKey = "1281372178316872";

        final var aExists = this.transactionRepository().existsByIdempotencyKey(aIdempotencyKey);

        Assertions.assertFalse(aExists);
    }

    @Test
    void givenAValidExistsTransaction_whenCallSaveButVersionIsNotMatch_thenThrowsConflictException() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                "1238712712678368126834"
        );

        final var aSavedTransaction = this.transactionRepository().save(aTransaction);

        final var expectedErrorMessage = "Transaction with identifier %s and version 2 does not match, transaction was updated by another transaction"
                .formatted(aTransaction.getId().value());

        Assertions.assertEquals(1, countTransactions());

        aSavedTransaction.complete();
        aSavedTransaction.incrementVersion(); // Simulate version mismatch

        final var aTransactionRepositoryVariable = this.transactionRepository(); // Variable to use in lambda, because this.customerRepository() is not allowed in lambda
        // this is a way to test if the exception is thrown, THIS IS A SCAM, in future disable this rule in sonar
        final var aException = Assertions.assertThrows(ConflictException.class,
                () -> aTransactionRepositoryVariable.save(aTransaction));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

}
