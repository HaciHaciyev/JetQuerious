package io.github.hacihaciyev.types;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface Setter {
    void set(PreparedStatement stmt, Object param, int idx) throws SQLException, TypeInlineException;
}