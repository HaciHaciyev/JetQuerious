package io.github.hacihaciyev.sql;

import static java.util.Objects.requireNonNull;

import io.github.hacihaciyev.schema.internal.Table;

public record JQ(Table[] schema, String sql) {
    public JQ {
        requireNonNull(sql, "SQL string representation cannot be null for JQ");
        schema = requireNonNull(schema, "Schema cannot be null for JQ").clone();
    }

    public Table[] schema() {
        return schema.clone();
    }

    @Override
    public String toString() {
        return sql;
    }
}
