package com.hadzhy.jdbclight.sql;

import java.util.Objects;

import static com.hadzhy.jdbclight.sql.Util.deleteSurplusComa;

public class ChainedFromBuilder {
    private final StringBuilder query;

    ChainedFromBuilder(StringBuilder query) {
        this.query = query;
    }

    public FunctionBuilder count(String column) {
        query.append("COUNT");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder sum(String column) {
        query.append("SUM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder avg(String column) {
        query.append("AVG");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder min(String column) {
        query.append("MIN");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder max(String column) {
        query.append("MAX");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder upper(String column) {
        query.append("UPPER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder lower(String column) {
        query.append("LOWER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder concat(String... columns) {
        if (columns.length == 0) throw new IllegalArgumentException("At least one column must be provided.");

        query.append("CONCAT(");

        if (columns.length == 1) query.append(columns[0]);
        else query.append(String.join(", ", columns));

        deleteSurplusComa(query);

        query.append(") ");
        return new FunctionBuilder(query);
    }

    public FunctionBuilder length(String column) {
        query.append("LENGTH");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder trim(String column) {
        query.append("TRIM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    private void appendColumn(String column) {
        Objects.requireNonNull(column, "Column can`t be null.");
        if (column.isBlank()) throw new IllegalArgumentException("Column can`t be blank.");

        query.append("(").append(column).append(") ");
    }

    public InitialWhereBuilder from(String table) {
        query.append("FROM ").append(table).append(" ");
        return new InitialWhereBuilder(query);
    }

    public JoinBuilder fromAs(String table, String alias) {
        query.append(table).append(" AS ").append(alias).append(" ");
        return new JoinBuilder(query);
    }
}
