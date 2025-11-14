package io.github.hacihaciyev.jdbc;

record ColumnMeta(String name, SqlType type, boolean nullable) {

    public boolean isValid(Object value) {
        if (value == null) return nullable;
        return type.isSupportedType(value);
    }
}
