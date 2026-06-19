package io.github.hacihaciyev.types;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface Getter {
    Object get(ResultSet rs, String column) throws SQLException, TypeInlineException;
}