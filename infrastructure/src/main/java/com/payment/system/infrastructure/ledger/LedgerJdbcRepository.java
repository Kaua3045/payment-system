package com.payment.system.infrastructure.ledger;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.exceptions.ReservationNotPendingException;
import com.payment.system.domain.exceptions.SnapshotNotExistsException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.ledger.LedgerReservation;
import com.payment.system.domain.ledger.LedgerReservationId;
import com.payment.system.domain.ledger.ReservationStatus;
import com.payment.system.domain.transactions.TransactionId;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.jdbc.DatabaseClient;
import com.payment.system.infrastructure.jdbc.JdbcUtils;
import com.payment.system.infrastructure.jdbc.RowMap;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

// TODO h2 tests does not support RETURNING clause,
//  so we need to use a separate query to fetch the reservation
//  after updating its status. Consider using a different approach or
//  database for testing that supports this feature.
// TODO and create, confirm and cancel reservation need ledger snapshot created, on if not exists,
//  this methods returning insufficient funds exception, because the snapshot is not created yet,
//  decide if create snapshot on create account with event or sync or make update snapshot create
//  if not exists, or make a separate method to create snapshot on demand
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

        // TreeMap: ordem determinística por id
        final var entriesByAccount = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getAccountId().value().toString(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        for (final var accountEntries : entriesByAccount.values()) {
            accountEntries.forEach(this::updateSnapshot);
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
                        "amount", reservation.getAmount().amount()
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
        releaseReservation(transactionId, ReservationStatus.CONFIRMED, true);
    }

    @Override
    public void cancelReservation(final TransactionId transactionId) {
        releaseReservation(transactionId, ReservationStatus.CANCELLED, false);
    }

    @Override
    public void lockAccountsInOrder(final List<AccountId> accountIds) {
        final var ids = accountIds.stream()
                .map(id -> id.value().toString())
                .distinct() // remove duplicates
                .sorted() // lock in deterministic order to avoid deadlocks
                .toList();

        for (final var id : ids) {
            this.databaseClient.queryOne(
                    """
                            SELECT account_id
                            FROM ledger_snapshots
                            WHERE account_id = :accountId
                            FOR UPDATE
                            """,
                    Map.of("accountId", id),
                    rs -> rs.getString("account_id")
            ).orElseThrow();
        }
    }

    @Override
    public Optional<LedgerReservation> findReservationByTransactionId(final TransactionId transactionId) {
        final var sql = "SELECT * FROM ledger_reservations WHERE transaction_id = :transactionId";
        return this.databaseClient.queryOne(sql, Map.of("transactionId", transactionId.value().toString()), mapReservation());
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

    private void releaseReservation(final TransactionId transactionId, final ReservationStatus newStatus, final boolean mustBePending) {
        final var txId = transactionId.value().toString();

        final var changed = this.databaseClient.update("""
                UPDATE ledger_reservations
                SET status = :status
                WHERE transaction_id = :transactionId
                    AND status = 'PENDING'
                """, Map.of("status", newStatus.name(), "transactionId", txId));

        if (changed == 0) {
            // Ja confirmada/cancelada/expirada (retry) ou inexistente: NUNCA libera de novo
            if (mustBePending) {
                throw new ReservationNotPendingException();
            }
            return;  // cancelar duas vezes é inofensivo
        }

        // changed == 1: a linha está travada por esta transação, então o SELECT é seguro
        final var reservation = this.databaseClient.queryOne(
                "SELECT account_id, amount FROM ledger_reservations WHERE transaction_id = :transactionId",
                Map.of("transactionId", txId),
                rs -> Map.of("accountId", rs.getString("account_id"), "amount", rs.getBigDecimal("amount"))
        ).orElseThrow();

        final var released = this.databaseClient.update("""
                UPDATE ledger_snapshots
                SET reserved_balance = reserved_balance - :amount,
                    updated_at = NOW()
                WHERE account_id = :accountId
                """, Map.of("accountId", reservation.get("accountId"), "amount", reservation.get("amount")));

        if (released != 1) {
            throw new SnapshotNotExistsException(reservation.get("accountId").toString());
        }
    }

    private void updateSnapshot(final LedgerEntry entry) {
        final var sql = """
                UPDATE ledger_snapshots
                SET
                    balance = balance + :amount,
                    last_ledger_id = :ledgerId,
                    updated_at = NOW()
                WHERE account_id = :accountId
                """;

        final var updated = this.databaseClient.update(
                sql,
                Map.of(
                        "accountId", entry.getAccountId().value().toString(),
                        "amount", entry.getAmount().amount(),
                        "ledgerId", entry.getId().value().toString()
                )
        );

        if (updated != 1) {
            throw new SnapshotNotExistsException(entry.getAccountId().value().toString());
        }
    }

    private RowMap<LedgerReservation> mapReservation() {
        return rs -> LedgerReservation.with(
                new LedgerReservationId(Ulid.from(rs.getString("id"))),
                0L, // Versão não persistida no banco por enquanto
                new TransactionId(Ulid.from(rs.getString("transaction_id"))),
                new AccountId(Ulid.from(rs.getString("account_id"))),
                Money.of(rs.getBigDecimal("amount")),
                JdbcUtils.getInstant(rs, "created_at"),
                JdbcUtils.getInstant(rs, "expires_at"),
                ReservationStatus.valueOf(rs.getString("status"))
        );
    }

    private Map<String, Object> toParams(final LedgerEntry entry) {
        final var params = new HashMap<String, Object>();
        params.put("id", entry.getId().value().toString());
        params.put("accountId", entry.getAccountId().value().toString());
        params.put("transactionId", entry.getTransactionId().value().toString());
        params.put("amount", entry.getAmount().amount());
        params.put("type", entry.getType().name());
        params.put("createdAt", OffsetDateTime.ofInstant(entry.getCreatedAt(), ZoneOffset.UTC));
        return params;
    }

    private Map<String, Object> toParams(final LedgerReservation reservation) {
        final var params = new HashMap<String, Object>();
        params.put("id", reservation.getId().value().toString());
        params.put("transactionId", reservation.getTransactionId().value().toString());
        params.put("accountId", reservation.getAccountId().value().toString());
        params.put("amount", reservation.getAmount().amount());
        params.put("status", reservation.getStatus().name());
        params.put("createdAt", OffsetDateTime.ofInstant(reservation.getCreatedAt(), ZoneOffset.UTC));
        params.put("expiresAt", OffsetDateTime.ofInstant(reservation.getExpiresAt(), ZoneOffset.UTC));
        return params;
    }
}
