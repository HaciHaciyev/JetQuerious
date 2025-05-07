package com.hadzhy.jetquerious.sql;

import java.util.Objects;

import static com.hadzhy.jetquerious.sql.Util.deleteSurplusComa;

public class FromBuilder {
    private final StringBuilder query;

    FromBuilder(StringBuilder query) {
        this.query = query;
    }

    public CaseBuilder caseStatement() {
        return new CaseBuilder(query.append(", "));
    }

    public JoinBuilder from(String table) {
        query.append("FROM ").append(table).append(" ");
        return new JoinBuilder(query);
    }

    public JoinBuilder fromAs(String table, String alias) {
        query.append("FROM ").append(table).append(" AS ").append(alias).append(" ");
        return new JoinBuilder(query);
    }

    public FunctionBuilder count(String column) {
        query.append(", COUNT");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder sum(String column) {
        query.append(", SUM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder avg(String column) {
        query.append(", AVG");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder min(String column) {
        query.append(", MIN");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder max(String column) {
        query.append(", MAX");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder upper(String column) {
        query.append(", UPPER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder lower(String column) {
        query.append(", LOWER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder concat(String... columns) {
        if (columns.length == 0) throw new IllegalArgumentException("At least one column must be provided.");
        query.append(", CONCAT(");

        if (columns.length == 1) query.append(columns[0]);
        else query.append(String.join(", ", columns));
        deleteSurplusComa(query);

        query.append(") ");
        return new FunctionBuilder(query);
    }

    public FunctionBuilder length(String column) {
        query.append(", LENGTH");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder trim(String column) {
        query.append(", TRIM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    private void appendColumn(String column) {
        Objects.requireNonNull(column, "Column can`t be null.");
        if (column.isBlank()) throw new IllegalArgumentException("Column can`t be blank.");
        query.append("(").append(column).append(") ");
    }
}
