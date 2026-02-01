package com.payment.system.infrastructure.pixkeys;

import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.pixkeys.PixKey;
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
public class PixKeyJdbcRepository implements PixKeyRepository {

    private static final Logger log = LoggerFactory.getLogger(PixKeyJdbcRepository.class);

    private final DatabaseClient databaseClient;

    public PixKeyJdbcRepository(final DatabaseClient databaseClient) {
        this.databaseClient = Objects.requireNonNull(databaseClient);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public PixKey save(final PixKey pixKey) {
        if (pixKey.getVersion() == 0) {
            log.info("Creating a new pix key with ID: {}", pixKey.getId().value().toString());
            create(pixKey);
            log.info("Pix key created with ID: {}", pixKey.getId().value().toString());
        }

        pixKey.incrementVersion();
        return pixKey;
    }

    @Override
    public boolean existsByValue(final String value) {
        final var aSql = "SELECT COUNT(*) FROM pix_keys WHERE key_value = :value";
        return this.databaseClient.count(aSql, Map.of("value", value)) > 0;
    }

    private void create(final PixKey aPixKey) {
        final var aSql = """
                INSERT INTO pix_keys (id, type, key_value, account_id, status, created_at, updated_at, deleted_at, version)
                VALUES (:id, :type, :value, :accountId, :status, :createdAt, :updatedAt, :deletedAt, (:version + 1))
                """;

        executeUpdate(aSql, aPixKey);
    }

    private int executeUpdate(final String aSql, final PixKey aPixKey) {
        final var aParams = new HashMap<String, Object>();
        aParams.put("id", aPixKey.getId().value().toString());
        aParams.put("version", aPixKey.getVersion());
        aParams.put("type", aPixKey.getType().name());
        aParams.put("value", aPixKey.getValue());
        aParams.put("accountId", aPixKey.getAccountId().value().toString());
        aParams.put("status", aPixKey.getStatus().name());
        aParams.put("createdAt", aPixKey.getCreatedAt());
        aParams.put("updatedAt", aPixKey.getUpdatedAt());
        aParams.put("deletedAt", aPixKey.getDeletedAt().orElse(null));

        return this.databaseClient.update(aSql, aParams);
    }
}
