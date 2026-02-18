package com.payment.system.infrastructure.transactions;

import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.PaginationMetadata;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.*;
import com.payment.system.domain.utils.ULID;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.exceptions.ConflictException;
import com.payment.system.infrastructure.jdbc.DatabaseClient;
import com.payment.system.infrastructure.jdbc.JdbcUtils;
import com.payment.system.infrastructure.jdbc.RowMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public Optional<Transaction> transactionOfIdempotencyKey(final String idempotencyKey) {
        final var aSql = "SELECT * FROM transactions WHERE idempotency_key = :idempotencyKey";
        return this.databaseClient.queryOne(aSql, Map.of("idempotencyKey", idempotencyKey), transactionMapper());
    }

    @Override
    public Optional<Transaction> transactionOfIdAndAccountId(final String transactionId, final String accountId) {
        final var aSql = "SELECT * FROM transactions WHERE id = :transactionId AND (from_account_id = :accountId OR to_account_id = :accountId);";
        return this.databaseClient.queryOne(aSql, Map.of("transactionId", transactionId, "accountId", accountId), transactionMapper());
    }

    @Override
    public Pagination<Transaction> listAll(final SearchQuery query) {
        final var sql = new StringBuilder("""
                    SELECT *
                    FROM transactions
                    WHERE 1=1
                """);

        final var countSql = new StringBuilder("""
                    SELECT COUNT(*)
                    FROM transactions
                    WHERE 1=1
                """);

        final Map<String, Object> params = new HashMap<>();

        // accountId nedded
        final var accountId = query.filters().get("accountId");
        if (accountId == null || accountId.isBlank()) {
            throw DomainException.with("Filter accountId is required");
        }

        sql.append("""
                    AND (from_account_id = :accountId OR to_account_id = :accountId)
                """);

        countSql.append("""
                    AND (from_account_id = :accountId OR to_account_id = :accountId)
                """);

        params.put("accountId", accountId);

        // textual search
        if (query.terms() != null && !query.terms().isBlank()) {
            sql.append("""
                        AND (
                            id ILIKE :terms
                            OR idempotency_key ILIKE :terms
                        )
                    """);

            countSql.append("""
                        AND (
                            id ILIKE :terms
                            OR idempotency_key ILIKE :terms
                        )
                    """);

            params.put("terms", "%" + query.terms() + "%");
        }

        // dynamic filters
        applyFilters(query.filters(), sql, countSql, params);

        // period
        query.getPeriod().ifPresent(period -> {
            sql.append("""
                        AND created_at BETWEEN :start AND :end
                    """);

            countSql.append("""
                        AND created_at BETWEEN :start AND :end
                    """);

            params.put(
                    "start",
                    OffsetDateTime.ofInstant(period.start(), ZoneOffset.UTC)
            );
            params.put(
                    "end",
                    OffsetDateTime.ofInstant(period.end(), ZoneOffset.UTC)
            );
        });

        // security ordenation
        sql.append(buildOrderBy(query));

        // pagination
        sql.append(" LIMIT :limit OFFSET :offset ");
        params.put("limit", query.perPage());
        params.put("offset", query.page() * query.perPage());

        final var items = this.databaseClient.query(
                sql.toString(),
                params,
                transactionMapper()
        );

        final var total = this.databaseClient.count(
                countSql.toString(),
                params
        );

        final var totalPages = (int) Math.ceil(
                (double) total / query.perPage()
        );

        return new Pagination<>(
                new PaginationMetadata(
                        query.page(),
                        query.perPage(),
                        totalPages,
                        total
                ),
                items
        );
    }

    private void create(final Transaction transaction) {
        final var aSql = """
                INSERT INTO transactions (id, from_account_id, to_account_id, pix_key_id, amount, status, type, source, idempotency_key, failure_reason, created_at, updated_at, version)
                VALUES (:id, :fromAccountId, :toAccountId, :pixKeyId, :amount, :status, :type, :source, :idempotencyKey, :failureReason, :createdAt, :updatedAt, (:version +1))
                """;

        try {
            executeUpdate(aSql, transaction);
        } catch (Exception ex) {
            throw ConflictException.with(
                    "Transaction with idempotencyKey %s already exists"
                            .formatted(transaction.getIdempotencyKey())
            );
        }
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
        aParams.put("source", aTransaction.getSource().name());
        aParams.put("idempotencyKey", aTransaction.getIdempotencyKey());
        aParams.put("failureReason", aTransaction.getFailureReason().orElse(null));
        aParams.put("createdAt",
                OffsetDateTime.ofInstant(aTransaction.getCreatedAt(), ZoneOffset.UTC));
        aParams.put("updatedAt",
                OffsetDateTime.ofInstant(aTransaction.getUpdatedAt(), ZoneOffset.UTC));
        aParams.put("version", aTransaction.getVersion());

        return this.databaseClient.update(aSql, aParams);
    }

    private RowMap<Transaction> transactionMapper() {
        return rs -> Transaction.with(
                new TransactionId(ULID.fromString(rs.getString("id"))),
                rs.getLong("version"),
                new AccountId(ULID.fromString(rs.getString("from_account_id"))),
                new AccountId(ULID.fromString(rs.getString("to_account_id"))),
                new PixKeyId(ULID.fromString(rs.getString("pix_key_id"))),
                new Money(rs.getBigDecimal("amount")),
                TransactionStatus.from(rs.getString("status")).orElse(null),
                TransactionType.from(rs.getString("type")).orElse(null),
                DepositSource.from(rs.getString("source")).orElse(null),
                rs.getString("idempotency_key"),
                rs.getString("failure_reason"),
                JdbcUtils.getInstant(rs, "created_at"),
                JdbcUtils.getInstant(rs, "updated_at")
        );
    }

    private void applyFilters(
            final Map<String, String> filters,
            final StringBuilder sql,
            final StringBuilder countSql,
            final Map<String, Object> params
    ) {
        filters.forEach((key, value) -> {
            switch (key) {
                case "status" -> {
                    sql.append(" AND status = :status ");
                    countSql.append(" AND status = :status ");
                    params.put("status", value.toUpperCase());
                }

                case "type" -> {
                    sql.append(" AND type = :type ");
                    countSql.append(" AND type = :type ");
                    params.put("type", value.toUpperCase());
                }

                case "source" -> {
                    sql.append(" AND source = :source ");
                    countSql.append(" AND source = :source ");
                    params.put("source", value.toUpperCase());
                }

                case "fromAccountId" -> {
                    sql.append(" AND from_account_id = :fromAccountId ");
                    countSql.append(" AND from_account_id = :fromAccountId ");
                    params.put("fromAccountId", value);
                }

                case "toAccountId" -> {
                    sql.append(" AND to_account_id = :toAccountId ");
                    countSql.append(" AND to_account_id = :toAccountId ");
                    params.put("toAccountId", value);
                }
            }
        });
    }

    private String buildOrderBy(final SearchQuery query) {

        final var sort = switch (query.sort()) {
            case "updatedAt" -> "updated_at";
            case "amount" -> "amount";
            case "status" -> "status";
            default -> "created_at";
        };

        final var direction =
                "desc".equalsIgnoreCase(query.direction())
                        ? "DESC"
                        : "ASC";

        return " ORDER BY " + sort + " " + direction + " ";
    }
}
