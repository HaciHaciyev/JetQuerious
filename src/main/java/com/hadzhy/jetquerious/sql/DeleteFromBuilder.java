package com.hadzhy.jetquerious.sql;

public class DeleteFromBuilder {
    private final StringBuilder query;

    DeleteFromBuilder(StringBuilder query) {
        this.query = query;
    }

    public WhereDeleteBuilder where(String condition) {
        query.append("WHERE ").append(condition).append(" ");
        return new WhereDeleteBuilder(query);
    }

    public WhereDeleteBuilder whereNot(String condition) {
        query.append("WHERE NOT ").append(condition).append(" ");
        return new WhereDeleteBuilder(query);
    }

    public WhereDeleteBuilder whereIn(String column, String subQuery) {
        query.append("WHERE ").append(column).append(" IN (").append(subQuery).append(") ");
        return new WhereDeleteBuilder(query);
    }

    public SQLState build() {
        return new SQLState(this.query.toString());
    }
}
