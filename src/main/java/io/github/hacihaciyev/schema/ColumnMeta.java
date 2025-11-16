package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.sql.SqlType;

public record ColumnMeta(String name, SqlType type, boolean nullable) {

    public boolean isValid(Object value) {
        if (value == null) return nullable;
        return type.isSupportedType(value);
    }
}
