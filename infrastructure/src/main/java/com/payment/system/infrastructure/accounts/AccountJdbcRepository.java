package com.payment.system.infrastructure.accounts;

import com.github.f4b6a3.ulid.Ulid;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.domain.exceptions.ConflictException;
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
public class AccountJdbcRepository implements AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountJdbcRepository.class);

    private final DatabaseClient databaseClient;

    public AccountJdbcRepository(final DatabaseClient databaseClient) {
        this.databaseClient = Objects.requireNonNull(databaseClient);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Account save(final Account anAccount) {
        if (anAccount.getVersion() == 0) {
            log.debug("Creating a new account with ID: {}", anAccount.getId().value().toString());
            create(anAccount);
            log.info("Account created with ID: {}", anAccount.getId().value().toString());
        } else {
            log.debug("Updating account with ID: {}", anAccount.getId().value().toString());
            update(anAccount);
            log.info("Updated account with ID: {}", anAccount.getId().value().toString());
        }

        anAccount.incrementVersion();
        return anAccount;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void applyTransfer(final Account fromAccount, final Account toAccount) {
        final var aSql = """
                UPDATE accounts
                SET balance = CASE
                        WHEN id = :fromId THEN :fromBalance
                        WHEN id = :toId THEN :toBalance
                    END,
                    version = version + 1,
                    updated_at = :updatedAt
                WHERE (id = :fromId AND version = :fromVersion)
                     OR (id = :toId AND version = :toVersion)
                """;

        final var aParams = new HashMap<String, Object>();
        aParams.put("fromId", fromAccount.getId().value().toString());
        aParams.put("toId", toAccount.getId().value().toString());
        aParams.put("fromBalance", fromAccount.getBalance().amount());
        aParams.put("toBalance", toAccount.getBalance().amount());
        aParams.put("updatedAt", OffsetDateTime.ofInstant(fromAccount.getUpdatedAt(), ZoneOffset.UTC));
        aParams.put("fromVersion", fromAccount.getVersion());
        aParams.put("toVersion", toAccount.getVersion());

        final var aRowsAffected = this.databaseClient.update(aSql, aParams);

        if (aRowsAffected != 2) {
            throw ConflictException.with("Optimistic lock failure on transfer, one of the accounts was updated by another transaction");
        }
    }

    @Override
    public Optional<Account> accountOfId(final String anId) {
        final var aSql = "SELECT * FROM accounts WHERE id = :id";
        return this.databaseClient.queryOne(aSql, Map.of("id", anId), accountMapper());
    }

    private void create(final Account anAccount) {
        final var aSql = """
                INSERT INTO accounts (id, user_id, balance, status, created_at, updated_at, closed_at, version)
                VALUES (:id, :userId, :balance, :status, :createdAt, :updatedAt, :closedAt, (:version + 1))
                """;

        executeUpdate(aSql, anAccount);
    }

    private void update(final Account anAccount) {
        final var aSql = """
                UPDATE accounts
                SET version = :version + 1, balance = :balance, status = :status, updated_at = :updatedAt, closed_at = :closedAt
                WHERE id = :id AND version = :version
                """;

        if (executeUpdate(aSql, anAccount) == 0) {
            log.warn("Optimistic lock failure on account update accountId={} userId={} version={}",
                    anAccount.getId().value().toString(),
                    anAccount.getUserId(),
                    anAccount.getVersion()
            );

            throw ConflictException.with("Account with identifier %s and version %d does not match, account was updated by another transaction"
                    .formatted(anAccount.getId().value(), anAccount.getVersion()));
        }
    }

    private int executeUpdate(final String aSql, final Account anAccount) {
        final var aParams = new HashMap<String, Object>();
        aParams.put("id", anAccount.getId().value().toString());
        aParams.put("userId", anAccount.getUserId());
        aParams.put("balance", anAccount.getBalance().amount());
        aParams.put("status", anAccount.getStatus().name());
        aParams.put("createdAt",
                OffsetDateTime.ofInstant(anAccount.getCreatedAt(), ZoneOffset.UTC));
        aParams.put("updatedAt",
                OffsetDateTime.ofInstant(anAccount.getUpdatedAt(), ZoneOffset.UTC));
        aParams.put("closedAt",
                anAccount.getClosedAt()
                        .map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC))
                        .orElse(null));

        aParams.put("version", anAccount.getVersion());

        return this.databaseClient.update(aSql, aParams);
    }

    private RowMap<Account> accountMapper() {
        return rs -> Account.with(
                new AccountId(Ulid.from(rs.getString("id"))),
                rs.getLong("version"),
                rs.getString("user_id"),
                new Money(rs.getBigDecimal("balance")),
                AccountStatus.from(rs.getString("status")).orElse(null),
                JdbcUtils.getInstant(rs, "created_at"),
                JdbcUtils.getInstant(rs, "updated_at"),
                JdbcUtils.getInstant(rs, "closed_at")
        );
    }
}
