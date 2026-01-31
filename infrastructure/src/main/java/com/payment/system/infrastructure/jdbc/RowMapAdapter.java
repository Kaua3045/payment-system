package com.payment.system.infrastructure.jdbc;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class RowMapAdapter<T> implements RowMapper<T> {

    private final RowMap<T> target;

    public RowMapAdapter(final RowMap<T> target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public T mapRow(@NonNull final ResultSet rs, final int rowNum) throws SQLException {
        return this.target.mapRow(rs);
    }
}
