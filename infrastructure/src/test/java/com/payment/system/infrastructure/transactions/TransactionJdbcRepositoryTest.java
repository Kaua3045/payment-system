package com.payment.system.infrastructure.transactions;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.exceptions.ConflictException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

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

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                "1238712712678368126834"
        );

        this.transactionRepository().save(aTransaction);

        final var aIdempotencyKey = aTransaction.getIdempotencyKey();

        final var aExists = this.transactionRepository().existsByIdempotencyKey(aIdempotencyKey);

        Assertions.assertTrue(aExists);
    }

    @Test
    void givenAValidExistsIdempotencyKey_whenCallsExistsByIdempotencyKey_thenShouldReturnFalse() {
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

    @Test
    void givenAValidIdempotencyKey_whenCallsTransactionOfIdempotencyKey_thenReturnTransaction() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                "1238712712678368126834"
        );

        this.transactionRepository().save(aTransaction);

        Assertions.assertEquals(1, countTransactions());

        final var aSavedTransaction = this.transactionRepository().transactionOfIdempotencyKey(aTransaction.getIdempotencyKey()).get();

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
    @Sql(statements = {
            "INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, idempotency_key, failure_reason, created_at, updated_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', '01KGB053FZJ0PC00HD9QZWAJ1H', '01KGB053FZJ0PC00HD9QZWAJ2H', '01KGB053FZJ0PC00HD9QZWAJ3H', 10, 'INVALID', 'TRANSFER', '1712687316266128', NULL, NOW(), NOW(), 1)"
    })
    void givenAnInvalidTransactionStatusInDB_whenCallsTransactionOfIdempotencyKey_thenShouldReturnIt() {
        Assertions.assertEquals(1, countTransactions());

        final var expectedErrorMessage = "should not be null";
        final var expectedErrorProperty = "status";

        final var aException = Assertions.assertThrows(ValidationException.class,
                () -> this.transactionRepository().transactionOfIdempotencyKey("1712687316266128"));

        Assertions.assertEquals(1, aException.getErrors().size());
        Assertions.assertEquals(expectedErrorMessage, aException.getErrors().getFirst().message());
        Assertions.assertEquals(expectedErrorProperty, aException.getErrors().getFirst().property());
    }

    @Test
    @Sql(statements = {
            "INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, idempotency_key, failure_reason, created_at, updated_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', '01KGB053FZJ0PC00HD9QZWAJ1H', '01KGB053FZJ0PC00HD9QZWAJ2H', '01KGB053FZJ0PC00HD9QZWAJ3H', 10, 'PENDING', 'INVALID', '1712687316266128', NULL, NOW(), NOW(), 1)"
    })
    void givenAnInvalidTransactionTypeInDB_whenCallsTransactionOfIdempotencyKey_thenShouldReturnIt() {
        Assertions.assertEquals(1, countTransactions());

        final var expectedErrorMessage = "should not be null";
        final var expectedErrorProperty = "type";

        final var aException = Assertions.assertThrows(ValidationException.class,
                () -> this.transactionRepository().transactionOfIdempotencyKey("1712687316266128"));

        Assertions.assertEquals(1, aException.getErrors().size());
        Assertions.assertEquals(expectedErrorMessage, aException.getErrors().getFirst().message());
        Assertions.assertEquals(expectedErrorProperty, aException.getErrors().getFirst().property());
    }
}
