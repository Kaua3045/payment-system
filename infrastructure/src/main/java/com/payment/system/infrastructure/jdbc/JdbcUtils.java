package com.payment.system.infrastructure.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public final class JdbcUtils {

    private JdbcUtils() {}

    public static Instant getInstant(final ResultSet rs, final String prop) throws SQLException {
        if (prop == null || prop.isBlank()) {
            return null;
        }

        var timestamp = rs.getTimestamp(prop);

        if (timestamp == null) {
            return null;
        }

        return timestamp.toInstant();
    }
}
