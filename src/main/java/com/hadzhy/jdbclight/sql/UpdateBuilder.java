package com.hadzhy.jdbclight.sql;

public class UpdateBuilder {
    final StringBuilder query;

    private UpdateBuilder(StringBuilder query) {
        this.query = query;
    }

    static UpdateBuilder update(String table) {
        return new UpdateBuilder(new StringBuilder().append("UPDATE ").append(table).append(" "));
    }

    public ChainedUpdateBuilder set(String condition) {
        return new ChainedUpdateBuilder(query.append("SET ").append(condition).append(" "));
    }
}
