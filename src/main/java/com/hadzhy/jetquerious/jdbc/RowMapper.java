package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.RepositoryException;
import com.hadzhy.jetquerious.util.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {
    @Nullable
    T extractData(ResultSet rs, int rowNum) throws SQLException, RepositoryException;
}
