package com.payment.system.infrastructure.accounts;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
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
            log.info("Creating a new account with ID: {}", anAccount.getId().value().toString());
            create(anAccount);
            log.info("Account created with ID: {}", anAccount.getId().value().toString());
        } else {
            log.info("Updating account with ID: {}", anAccount.getId().value().toString());
            update(anAccount);
            log.info("Updated account with ID: {}", anAccount.getId().value().toString());
        }

        anAccount.incrementVersion();
        return anAccount;
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
                new AccountId(ULID.fromString(rs.getString("id"))),
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
