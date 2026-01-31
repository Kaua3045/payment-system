package com.payment.system.infrastructure.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMap<T> {

    T mapRow(ResultSet rs) throws SQLException;
}
