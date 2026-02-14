package com.payment.system.infrastructure.transactions;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.*;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.utils.Period;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.exceptions.ConflictException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Map;

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
                DepositSource.EXTERNAL,
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
    void givenAnInvalidNewTransactionWithExistIdempotencyKey_whenCallsSave_thenShouldThrows() {
        Assertions.assertEquals(0, countTransactions());

        final var expectedErrorMessage = "Transaction with idempotencyKey 1238712712678368126834 already exists";

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "1238712712678368126834"
        );

        this.transactionRepository().save(aTransaction);

        final var aException = Assertions.assertThrows(ConflictException.class, () ->
                this.transactionRepository().save(Transaction.newTransaction(
                        aTransaction.getFromAccountId(),
                        aTransaction.getToAccountId(),
                        aTransaction.getPixKeyId(),
                        aTransaction.getAmount(),
                        aTransaction.getType(),
                        aTransaction.getSource(),
                        aTransaction.getIdempotencyKey()
                )));

        Assertions.assertEquals(1, countTransactions());

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
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
                DepositSource.EXTERNAL,
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
    void givenAValidNotExistsIdempotencyKey_whenCallsExistsByIdempotencyKey_thenShouldReturnTrue() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
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
                DepositSource.EXTERNAL,
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
                DepositSource.EXTERNAL,
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
            "INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, source, idempotency_key, failure_reason, created_at, updated_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', '01KGB053FZJ0PC00HD9QZWAJ1H', '01KGB053FZJ0PC00HD9QZWAJ2H', '01KGB053FZJ0PC00HD9QZWAJ3H', 10, 'INVALID', 'TRANSFER', 'source', '1712687316266128', NULL, NOW(), NOW(), 1)"
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
            "INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, source, idempotency_key, failure_reason, created_at, updated_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', '01KGB053FZJ0PC00HD9QZWAJ1H', '01KGB053FZJ0PC00HD9QZWAJ2H', '01KGB053FZJ0PC00HD9QZWAJ3H', 10, 'PENDING', 'INVALID', 'ATM', '1712687316266128', NULL, NOW(), NOW(), 1)"
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

    @Test
    void givenAValidIds_whenCallsTransactionOfIdAndAccountId_thenReturnTransaction() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "1238712712678368126834"
        );

        this.transactionRepository().save(aTransaction);

        Assertions.assertEquals(1, countTransactions());

        final var aSavedTransaction = this.transactionRepository().transactionOfIdAndAccountId(
                aTransaction.getId().value().toString(),
                aTransaction.getFromAccountId().value().toString()
        ).get();

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
    void givenAnInvalidAccountId_whenCallsTransactionOfIdAndAccountId_thenReturnEmpty() {
        Assertions.assertEquals(0, countTransactions());

        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "1238712712678368126834"
        );

        this.transactionRepository().save(aTransaction);

        Assertions.assertEquals(1, countTransactions());

        final var aSavedTransaction = this.transactionRepository().transactionOfIdAndAccountId(
                aTransaction.getId().value().toString(),
                "17923712683168"
        );

        Assertions.assertTrue(aSavedTransaction.isEmpty());
    }

    @Test
    @Sql(statements = {
            "INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, source, idempotency_key, failure_reason, created_at, updated_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', '01KGB053FZJ0PC00HD9QZWAJ1H', '01KGB053FZJ0PC00HD9QZWAJ2H', '01KGB053FZJ0PC00HD9QZWAJ3H', 10, 'PENDING', 'TRANSFER', 'INVALID', '1712687316266128', NULL, NOW(), NOW(), 1)"
    })
    void givenAnInvalidDepositSourceInDB_whenCallsTransactionOfIdAndAccountId_thenShouldThrowsException() {
        Assertions.assertEquals(1, countTransactions());

        final var expectedErrorMessage = "should not be null";
        final var expectedErrorProperty = "source";

        final var aException = Assertions.assertThrows(ValidationException.class,
                () -> this.transactionRepository().transactionOfIdAndAccountId("01KGB053FZJ0PC00HD9QZWAJ0H", "01KGB053FZJ0PC00HD9QZWAJ1H"));

        Assertions.assertEquals(1, aException.getErrors().size());
        Assertions.assertEquals(expectedErrorMessage, aException.getErrors().getFirst().message());
        Assertions.assertEquals(expectedErrorProperty, aException.getErrors().getFirst().property());
    }

    @Test
    void givenNoAccountIdFilter_whenCallsListAll_thenShouldThrowException() {
        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc"
        );

        final var exception = Assertions.assertThrows(
                DomainException.class,
                () -> this.transactionRepository().listAll(query)
        );

        Assertions.assertEquals("Filter accountId is required", exception.getMessage());
    }

    @Test
    void givenNoTransactions_whenCallsListAll_thenShouldReturnEmptyPagination() {
        final var accountId = IdentifierUtils.generateNewMonotonicULID().toString();

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of("accountId", accountId)
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertTrue(result.items().isEmpty());
        Assertions.assertEquals(0, result.metadata().totalItems());
        Assertions.assertEquals(0, result.metadata().totalPages());
    }

    @Test
    void givenTransactions_whenFilterByAccountId_thenShouldReturnFromOrToMatches() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var otherAccount = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var tx1 = Transaction.newTransaction(
                accountId,
                otherAccount,
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal(10)),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "idemp-1"
        );

        final var tx2 = Transaction.newTransaction(
                otherAccount,
                accountId,
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal(20)),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "idemp-2"
        );

        this.transactionRepository().save(tx1);
        this.transactionRepository().save(tx2);

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of("accountId", accountId.value().toString())
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertEquals(2, result.items().size());
    }

    @Test
    void givenMultipleTransactions_whenPaginate_thenShouldReturnCorrectPage() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        for (int i = 0; i < 15; i++) {
            this.transactionRepository().save(
                    Transaction.newTransaction(
                            accountId,
                            new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                            new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                            new Money(new BigDecimal(i + 1)),
                            TransactionType.TRANSFER,
                            DepositSource.EXTERNAL,
                            "idemp-" + i
                    )
            );
        }

        final var query = SearchQuery.newSearchQuery(
                1,
                10,
                null,
                "createdAt",
                "asc",
                Map.of("accountId", accountId.value().toString())
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertEquals(5, result.items().size());
        Assertions.assertEquals(15, result.metadata().totalItems());
        Assertions.assertEquals(2, result.metadata().totalPages());
    }

    @Test
    void givenTransactions_whenFilterByStatus_thenShouldReturnOnlyMatching() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var tx = Transaction.newTransaction(
                accountId,
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal(10)),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "idemp-status"
        );

        tx.complete();

        this.transactionRepository().save(tx);

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of(
                        "accountId", accountId.value().toString(),
                        "status", "COMPLETED"
                )
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
    }

    @Test
    void givenTransactions_whenFilterByType_thenShouldReturnOnlyThatType() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        this.transactionRepository().save(
                Transaction.newTransaction(
                        accountId,
                        new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                        new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                        new Money(new BigDecimal(10)),
                        TransactionType.TRANSFER,
                        DepositSource.EXTERNAL,
                        "idemp-type"
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of(
                        "accountId", accountId.value().toString(),
                        "type", "TRANSFER"
                )
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
    }

    @Test
    void givenTransactions_whenFilterBySource_thenShouldReturnOnlyMatching() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        this.transactionRepository().save(
                Transaction.newTransaction(
                        accountId,
                        new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                        new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                        new Money(new BigDecimal(10)),
                        TransactionType.TRANSFER,
                        DepositSource.EXTERNAL,
                        "idemp-source"
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of(
                        "accountId", accountId.value().toString(),
                        "source", "EXTERNAL"
                )
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
    }

    @Test
    void givenTransactions_whenFilterByPeriod_thenShouldReturnOnlyInsidePeriod() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var oldDate = InstantUtils.now().minusSeconds(86400 * 5);
        final var newDate = InstantUtils.now();

        final var oldTx = Transaction.with(
                new TransactionId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                accountId,
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal(10)),
                TransactionStatus.PENDING,
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "old-idemp",
                null,
                oldDate,
                oldDate
        );

        final var newTx = Transaction.newTransaction(
                accountId,
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal(20)),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "new-idemp"
        );

        this.transactionRepository().save(oldTx);
        this.transactionRepository().save(newTx);

        final var period = new Period(
                InstantUtils.now().minusSeconds(3600),
                InstantUtils.now().plusSeconds(3600)
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                period,
                Map.of("accountId", accountId.value().toString())
        );

        final var result = this.transactionRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
    }
}
