package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.types.SQLType;

public record ColumnMeta(String name, SQLType type, boolean nullable) {

    public boolean isValid(Object value) {
        if (value == null) return nullable;
        return type.isSupportedType(value);
    }
}
