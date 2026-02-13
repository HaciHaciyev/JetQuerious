package io.github.hacihaciyev.jdbc;


import io.github.hacihaciyev.sql_error_translation.RepositoryException;
import io.github.hacihaciyev.util.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetExtractor<T> {
    @Nullable
    T extractData(ResultSet rs) throws SQLException, RepositoryException;
}