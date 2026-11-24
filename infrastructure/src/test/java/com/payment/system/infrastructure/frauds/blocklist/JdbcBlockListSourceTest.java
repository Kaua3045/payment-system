package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.AbstractRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcBlockListSourceTest extends AbstractRepositoryTest {

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM fraud_blocklist").update();
    }

    @Test
    void shouldLoadNonExpiredEntries() {
        insert("acc", "123", null);
        insert("key", "abc", Instant.now().plus(1, ChronoUnit.HOURS));
        insert("card", "456", Instant.now().minus(1, ChronoUnit.HOURS));

        Set<String> result = jdbcBlockListSource().load();

        assertEquals(Set.of(
                "acc:123",
                "key:abc"
        ), result);
    }

    @Test
    void shouldReturnEmptySetWhenNoValidEntriesExist() {
        insert("acc", "123", Instant.now().minus(1, ChronoUnit.HOURS));

        Set<String> result = jdbcBlockListSource().load();

        assertEquals(Set.of(), result);
    }

    @Test
    void shouldLoadAllEntriesWithoutExpiration() {
        insert("acc", "123", null);
        insert("key", "abc", null);

        Set<String> result = jdbcBlockListSource().load();

        assertEquals(Set.of("acc:123", "key:abc"), result);
    }

    private void insert(String type, String value, Instant expiresAt) {
        jdbcClient.sql("""
                INSERT INTO fraud_blocklist (
                    blocked_type,
                    blocked_value,
                    expires_at
                )
                VALUES (
                    :type,
                    :value,
                    :expiresAt
                )
                """)
                .param("type", type)
                .param("value", value)
                .param("expiresAt", expiresAt == null ? null :
                        java.sql.Timestamp.from(expiresAt))
                .update();
    }

}