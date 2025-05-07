package com.hadzhy.jetquerious.sql;

public class WhereUpdateBuilder {
    private final StringBuilder query;

    WhereUpdateBuilder(StringBuilder query) {
        this.query = query;
    }

    public WhereUpdateBuilder and(String condition) {
        query.append("AND ").append(condition).append(" ");
        return this;
    }

    public WhereUpdateBuilder or(String condition) {
        query.append("OR ").append(condition).append(" ");
        return this;
    }

    public SQLState returning(String... columns) {
        return new SQLState(this.query.append("RETURNING ").append(String.join(", ", columns)).append(" ").toString());
    }

    public SQLState build() {
        return new SQLState(this.query.toString());
    }
}
