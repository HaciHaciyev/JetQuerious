package com.hadzhy.jdbclight.sql;

import java.util.Objects;

import static com.hadzhy.jdbclight.sql.Util.deleteSurplusComa;

public class SelectBuilder {
    final StringBuilder query;

    private SelectBuilder(StringBuilder query) {
        this.query = query;
    }

    static SelectBuilder with(String table, SQLQuery subQuery) {
        return new SelectBuilder(new StringBuilder("WITH ")
                .append(table)
                .append(" AS ")
                .append("(")
                .append(subQuery.sql()).append(") ")
                .append("SELECT "));
    }

    static SelectBuilder with(StringBuilder query, String table, SQLQuery subQuery) {
        return new SelectBuilder(query.append("WITH ").append(table).append(" AS ").append("(").append(subQuery.sql()).append(") ").append("SELECT "));
    }

    static SelectBuilder select() {
        return new SelectBuilder(new StringBuilder("SELECT "));
    }

    static SelectBuilder select(StringBuilder query) {
        return new SelectBuilder(query.append("SELECT "));
    }

    static SelectBuilder selectDistinct() {
        return new SelectBuilder(new StringBuilder("SELECT DISTINCT "));
    }

    static SelectBuilder selectDistinct(StringBuilder query) {
        return new SelectBuilder(query.append("SELECT DISTINCT "));
    }

    public CaseBuilder caseStatement() {
        return new CaseBuilder(query);
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

    public ColumnBuilder column(String column) {
        query.append(column).append(" ");
        return new ColumnBuilder(query, true);
    }

    public FromBuilder columns(String... columns) {
        if (columns.length == 0) return all();

        if (columns.length == 1) {
            String column = columns[0];
            query.append(column).append(" ");
            return new FromBuilder(query);
        }

        query.append(String.join(", ", columns)).append(" ");
        return new FromBuilder(query);
    }

    public FromBuilder all() {
        query.append("* ");
        return new FromBuilder(query);
    }
}
