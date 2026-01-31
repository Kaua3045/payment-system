package com.payment.system.infrastructure.accounts;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.infrastructure.jdbc.DatabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Objects;

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
        }

        anAccount.incrementVersion();
        return anAccount;
    }

    private void create(final Account anAccount) {
        final var aSql = """
                INSERT INTO accounts (id, user_id, balance, status, created_at, updated_at, closed_at, version)
                VALUES (:id, :userId, :balance, :status, :createdAt, :updatedAt, :closedAt, (:version + 1))
                """;

        executeUpdate(aSql, anAccount);
    }

    private int executeUpdate(final String aSql, final Account anAccount) {
        final var aParams = new HashMap<String, Object>();
        aParams.put("id", anAccount.getId().value().toString());
        aParams.put("userId", anAccount.getUserId());
        aParams.put("balance", anAccount.getBalance().amount());
        aParams.put("status", anAccount.getStatus().name());
        aParams.put("createdAt", anAccount.getCreatedAt());
        aParams.put("updatedAt", anAccount.getUpdatedAt());
        aParams.put("closedAt", anAccount.getClosedAt().orElse(null));
        aParams.put("version", anAccount.getVersion());

        return this.databaseClient.update(aSql, aParams);
    }
}
