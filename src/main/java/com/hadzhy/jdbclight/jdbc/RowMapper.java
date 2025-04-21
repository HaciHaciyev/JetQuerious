package com.hadzhy.jdbclight.jdbc;

import com.hadzhy.jdbclight.exceptions.RepositoryException;
import com.hadzhy.jdbclight.util.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {
    @Nullable
    T extractData(ResultSet rs, int rowNum) throws SQLException, RepositoryException;
}
