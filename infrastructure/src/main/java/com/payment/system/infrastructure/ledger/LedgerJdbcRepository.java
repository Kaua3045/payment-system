package com.payment.system.infrastructure.ledger;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.ledger.LedgerReservation;
import com.payment.system.domain.ledger.LedgerReservationId;
import com.payment.system.domain.ledger.ReservationStatus;
import com.payment.system.domain.transactions.TransactionId;
import com.payment.system.infrastructure.jdbc.DatabaseClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LedgerJdbcRepository implements LedgerRepository {

    private final DatabaseClient databaseClient;

    public LedgerJdbcRepository(final DatabaseClient databaseClient) {
        this.databaseClient = Objects.requireNonNull(databaseClient);
    }

    @Override
    public void save(final LedgerEntry entry) {
        final var sql = """
                INSERT INTO ledger_entries (id, account_id, transaction_id, amount, type, created_at)
                VALUES (:id, :accountId, :transactionId, :amount, :type, :createdAt)
                """;
        this.databaseClient.update(sql, toParams(entry));
        updateSnapshot(entry);
    }

    @Override
    public void saveAll(final List<LedgerEntry> entries) {
        final var sql = """
                INSERT INTO ledger_entries (id, account_id, transaction_id, amount, type, created_at)
                VALUES (:id, :accountId, :transactionId, :amount, :type, :createdAt)
                """;
        final var batchParams = entries.stream()
                .map(this::toParams)
                .collect(Collectors.toList());
        this.databaseClient.batchUpdate(sql, batchParams);

        for (final var entry : entries) {
            updateSnapshot(entry);
        }
    }

    @Override
    public BigDecimal calculateBalance(final AccountId accountId) {
        final var sql = """
            SELECT balance - reserved_balance
            FROM ledger_snapshots
            WHERE account_id = :accountId
            """;

        return this.databaseClient.queryOne(
                sql,
                Map.of("accountId", accountId.value().toString()),
                rs -> rs.getBigDecimal(1)
        ).orElse(BigDecimal.ZERO);
    }

    @Override
    public void createReservation(final LedgerReservation reservation) {
        final var sql = """
            UPDATE ledger_snapshots
            SET
                reserved_balance = reserved_balance + :amount,
                updated_at = NOW()
            WHERE account_id = :accountId
              AND balance - reserved_balance >= :amount
            """;

        final var updated = this.databaseClient.update(
                sql,
                Map.of(
                        "accountId", reservation.getAccountId().value().toString(),
                        "amount", reservation.getAmount()
                )
        );

        if (updated == 0) {
            throw new InsufficientFundsException();
        }

        final var insertSql = """
            INSERT INTO ledger_reservations (
                id,
                transaction_id,
                account_id,
                amount,
                status,
                created_at,
                expires_at
            )
            VALUES (
                :id,
                :transactionId,
                :accountId,
                :amount,
                :status,
                :createdAt,
                :expiresAt
            )
            """;

        this.databaseClient.update(
                insertSql,
                toParams(reservation)
        );
    }

    @Override
    public void confirmReservation(final TransactionId transactionId) {
        final var sql = """
            UPDATE ledger_reservations
            SET status = 'CONFIRMED'
            WHERE transaction_id = :transactionId
              AND status = 'PENDING'
            RETURNING account_id, amount
            """;

        final var reservation = this.databaseClient.queryOne(
                sql,
                Map.of("transactionId", transactionId.value().toString()),
                rs -> Map.of(
                        "accountId", rs.getString("account_id"),
                        "amount", rs.getBigDecimal("amount")
                )
        ).orElseThrow();

        final var releaseSql = """
            UPDATE ledger_snapshots
            SET
                reserved_balance = reserved_balance - :amount,
                updated_at = NOW()
            WHERE account_id = :accountId
            """;

        this.databaseClient.update(
                releaseSql,
                Map.of(
                        "accountId", reservation.get("accountId"),
                        "amount", reservation.get("amount")
                )
        );
    }

    @Override
    public void cancelReservation(final TransactionId transactionId) {
        final var sql = """
            UPDATE ledger_reservations
            SET status = 'CANCELLED'
            WHERE transaction_id = :transactionId
              AND status = 'PENDING'
            RETURNING account_id, amount
            """;

        final var reservation = this.databaseClient.queryOne(
                sql,
                Map.of("transactionId", transactionId.value().toString()),
                rs -> Map.of(
                        "accountId", rs.getString("account_id"),
                        "amount", rs.getBigDecimal("amount")
                )
        ).orElse(null);

        if (reservation == null) {
            return;
        }

        final var releaseSql = """
            UPDATE ledger_snapshots
            SET
                reserved_balance = reserved_balance - :amount,
                updated_at = NOW()
            WHERE account_id = :accountId
            """;

        this.databaseClient.update(
                releaseSql,
                Map.of(
                        "accountId", reservation.get("accountId"),
                        "amount", reservation.get("amount")
                )
        );
    }

    @Override
    public Optional<LedgerReservation> findReservationByTransactionId(final TransactionId transactionId) {
        final var sql = "SELECT * FROM ledger_reservations WHERE transaction_id = :transactionId";
        return this.databaseClient.queryOne(sql, Map.of("transactionId", transactionId.value().toString()), this::mapReservation);
    }

    // TODO implement this
//    @Override
//    public void expireReservations() {
//        final var sql = """
//            WITH candidates AS (
//                SELECT id
//                FROM ledger_reservations
//                WHERE status = 'PENDING'
//                  AND expires_at <= NOW()
//                ORDER BY expires_at
//                LIMIT 1000
//            ),
//            expired AS (
//                UPDATE ledger_reservations r
//                SET status = 'CANCELLED'
//                FROM candidates c
//                WHERE r.id = c.id
//                RETURNING r.account_id, r.amount
//            ),
//            released AS (
//                SELECT
//                    account_id,
//                    SUM(amount) AS amount
//                FROM expired
//                GROUP BY account_id
//            )
//            UPDATE ledger_snapshots s
//            SET
//                reserved_balance = s.reserved_balance - released.amount,
//                updated_at = NOW()
//            FROM released
//            WHERE s.account_id = released.account_id
//            """;
//
//        this.databaseClient.update(sql, Map.of());
//    }

    private void updateSnapshot(final LedgerEntry entry) {
        final var sql = """
            UPDATE ledger_snapshots
            SET
                balance = balance + :amount,
                last_ledger_id = :ledgerId,
                updated_at = NOW()
            WHERE account_id = :accountId
            """;

        this.databaseClient.update(
                sql,
                Map.of(
                        "accountId", entry.getAccountId().value().toString(),
                        "amount", entry.getAmount(),
                        "ledgerId", entry.getId().value().toString()
                )
        );
    }

    private LedgerReservation mapReservation(final ResultSet rs) throws SQLException {
        return LedgerReservation.with(
                new LedgerReservationId(Ulid.from(rs.getString("id"))),
                0L, // Versão não persistida no banco por enquanto
                new TransactionId(Ulid.from(rs.getString("transaction_id"))),
                new AccountId(Ulid.from(rs.getString("account_id"))),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                ReservationStatus.valueOf(rs.getString("status"))
        );
    }

    private Map<String, Object> toParams(final LedgerEntry entry) {
        final var params = new HashMap<String, Object>();
        params.put("id", entry.getId().value().toString());
        params.put("accountId", entry.getAccountId().value().toString());
        params.put("transactionId", entry.getTransactionId().value().toString());
        params.put("amount", entry.getAmount());
        params.put("type", entry.getType().name());
        params.put("createdAt", Timestamp.from(entry.getCreatedAt()));
        return params;
    }

    private Map<String, Object> toParams(final LedgerReservation reservation) {
        final var params = new HashMap<String, Object>();
        params.put("id", reservation.getId().value().toString());
        params.put("transactionId", reservation.getTransactionId().value().toString());
        params.put("accountId", reservation.getAccountId().value().toString());
        params.put("amount", reservation.getAmount());
        params.put("status", reservation.getStatus().name());
        params.put("createdAt", Timestamp.from(reservation.getCreatedAt()));
        params.put("expiresAt", Timestamp.from(reservation.getExpiresAt()));
        return params;
    }
}
