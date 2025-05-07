package com.hadzhy.jetquerious.jdbc;


import com.hadzhy.jetquerious.exceptions.RepositoryException;
import com.hadzhy.jetquerious.util.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetExtractor<T> {
    @Nullable
    T extractData(ResultSet rs) throws SQLException, RepositoryException;
}