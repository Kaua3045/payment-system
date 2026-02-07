package com.payment.system.infrastructure.pixkeys;

import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.*;
import com.payment.system.domain.utils.ULID;
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

    @Override
    public Optional<PixKey> pixKeyOfActiveByValue(final String value) {
        final var aSql = "SELECT * FROM pix_keys WHERE status = 'ACTIVE' AND key_value = :value";
        return this.databaseClient.queryOne(aSql, Map.of("value", value), pixKeyMapper());
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
        aParams.put("type", aPixKey.getKey().type().name());
        aParams.put("value", aPixKey.getKey().value());
        aParams.put("accountId", aPixKey.getAccountId().value().toString());
        aParams.put("status", aPixKey.getStatus().name());
        aParams.put("createdAt",
                OffsetDateTime.ofInstant(aPixKey.getCreatedAt(), ZoneOffset.UTC));
        aParams.put("updatedAt",
                OffsetDateTime.ofInstant(aPixKey.getUpdatedAt(), ZoneOffset.UTC));
        aParams.put("deletedAt",
                aPixKey.getDeletedAt()
                        .map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC))
                        .orElse(null));

        return this.databaseClient.update(aSql, aParams);
    }

    private RowMap<PixKey> pixKeyMapper() {
        return rs -> {
            final var aType = rs.getString("type");
            final var aPixKeyType = PixKeyType.from(aType)
                    .orElseThrow(() -> NotFoundException.with("PixKeyType %s not found".formatted(aType)));

            return PixKey.with(
                    new PixKeyId(ULID.fromString(rs.getString("id"))),
                    rs.getLong("version"),
                    new PixKeyValueFactory().create(
                            aPixKeyType,
                            rs.getString("key_value")),
                    new AccountId(ULID.fromString(rs.getString("account_id"))),
                    PixKeyStatus.from(rs.getString("status")).orElse(null),
                    JdbcUtils.getInstant(rs, "created_at"),
                    JdbcUtils.getInstant(rs, "updated_at"),
                    JdbcUtils.getInstant(rs, "deleted_at")
            );
        };
    }
}
