package com.hadzhy.jdbclight.sql;

public class ElseCaseBuilder {
    private final StringBuilder query;

    ElseCaseBuilder(StringBuilder query) {
        this.query = query;
    }

    public FromBuilder end() {
        query.append("END ");
        return new FromBuilder(query);
    }

    public FromBuilder endAs(String alias) {
        query.append("END AS ").append(alias).append(" ");
        return new FromBuilder(query);
    }
}
