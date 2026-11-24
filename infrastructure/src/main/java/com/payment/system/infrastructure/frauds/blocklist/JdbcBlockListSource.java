package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.infrastructure.jdbc.DatabaseClient;

import java.util.Set;

public class JdbcBlockListSource implements BlockListSource {

    private final DatabaseClient databaseClient;

    public JdbcBlockListSource(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Set<String> load() {
        return Set.copyOf(this.databaseClient.query("""
                SELECT blocked_value
                FROM fraud_blocklist
                WHERE expires_at is NULL
                    OR expires_at > CURRENT_TIMESTAMP
                """, (rs) -> rs.getString("blocked_value")));
    }
}
