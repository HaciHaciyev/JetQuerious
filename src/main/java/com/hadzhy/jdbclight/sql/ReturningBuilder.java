package com.hadzhy.jdbclight.sql;

public class ReturningBuilder {
    private final StringBuilder query;

    ReturningBuilder(StringBuilder query) {
        this.query = query;
    }

    public SQLState returning(String... columns) {
        return new SQLState(this.query.append("RETURNING ").append(String.join(", ", columns)).append(" ").toString());
    }

    public SQLState returning(String[] columns, String condition) {
        return new SQLState(this.query.append("RETURNING ").append(String.join(", ", columns)).append(" ").append(condition).append(" ").toString());
    }

    public SQLState returningAll() {
        return new SQLState(this.query.append("RETURNING *").toString());
    }

    public SQLState returningAll(String condition) {
        return new SQLState(this.query.append("RETURNING *").append(" ").append(condition).append(" ").toString());
    }

    public SQLState build() {
        return new SQLState(this.query.toString());
    }
}
