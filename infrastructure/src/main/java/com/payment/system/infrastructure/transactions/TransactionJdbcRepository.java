package com.payment.system.infrastructure.transactions;

import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.infrastructure.exceptions.ConflictException;
import com.payment.system.infrastructure.jdbc.DatabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class TransactionJdbcRepository implements TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(TransactionJdbcRepository.class);

    private final DatabaseClient databaseClient;

    public TransactionJdbcRepository(final DatabaseClient databaseClient) {
        this.databaseClient = Objects.requireNonNull(databaseClient);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Transaction save(final Transaction transaction) {
        if (transaction.getVersion() == 0) {
            log.info("Creating a new transaction wit ID: {}", transaction.getId().value().toString());
            create(transaction);
            log.info("Transaction created with ID: {}", transaction.getId().value().toString());
        } else {
            log.info("Updating transaction with ID: {}", transaction.getId().value().toString());
            update(transaction);
            log.info("Transaction updated with ID: {}", transaction.getId().value().toString());
        }

        transaction.incrementVersion();
        return transaction;
    }

    @Override
    public boolean existsByIdempotencyKey(final String idempotencyKey) {
        final var aSql = "SELECT COUNT(*) FROM transactions WHERE idempotency_key = :idempotencyKey";
        return this.databaseClient.count(aSql, Map.of("idempotencyKey", idempotencyKey)) > 0;
    }

    private void create(final Transaction transaction) {
        final var aSql = """
                INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, idempotency_key, failure_reason, created_at, updated_at, version)
                VALUES (:id, :fromAccountId, :toAccountId, :pixKeyId, :amount, :status, :type, :idempotencyKey, :failureReason, :createdAt, :updatedAt, (:version +1))
                """;

        executeUpdate(aSql, transaction);
    }

    private void update(final Transaction transaction) {
        final var aSql = """
                UPDATE transactions
                SET version = :version + 1, status = :status, updated_at = :updatedAt, failure_reason = :failureReason
                WHERE id = :id AND version = :version
                """;

        if (executeUpdate(aSql, transaction) == 0) {
            throw ConflictException.with("Transaction with identifier %s and version %d does not match, transaction was updated by another transaction"
                    .formatted(transaction.getId().value(), transaction.getVersion()));
        }
    }

    private int executeUpdate(final String aSql, final Transaction aTransaction) {
        final var aParams = new HashMap<String, Object>();
        aParams.put("id", aTransaction.getId().value().toString());
        aParams.put("fromAccountId", aTransaction.getFromAccountId().value().toString());
        aParams.put("toAccountId", aTransaction.getToAccountId().value().toString());
        aParams.put("pixKeyId", aTransaction.getPixKeyId().value().toString());
        aParams.put("amount", aTransaction.getAmount().amount());
        aParams.put("status", aTransaction.getStatus().name());
        aParams.put("type", aTransaction.getType().name());
        aParams.put("idempotencyKey", aTransaction.getIdempotencyKey());
        aParams.put("failureReason", aTransaction.getFailureReason().orElse(null));
        aParams.put("createdAt", aTransaction.getCreatedAt());
        aParams.put("updatedAt", aTransaction.getUpdatedAt());
        aParams.put("version", aTransaction.getVersion());

        return this.databaseClient.update(aSql, aParams);
    }
}
