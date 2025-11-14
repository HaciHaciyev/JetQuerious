package io.github.hacihaciyev.jdbc;

import java.util.Arrays;

record TableMeta(String table, String primaryKey, ColumnMeta[] columns) {

    public ColumnMeta[] columns() {
        return columns.clone();
    }

    public boolean isTableExists(String tableName) {
        return table.equalsIgnoreCase(tableName);
    }

    public boolean isColumnExists(String columnName) {
        return Arrays.stream(columns).anyMatch(column -> column.name().equalsIgnoreCase(columnName));
    }

    public boolean isPrimaryKeyExists(String pk) {
        return primaryKey.equalsIgnoreCase(pk);
    }
}