package com.hadzhy.jetquerious.sql;

import java.util.Objects;

import static com.hadzhy.jetquerious.sql.Util.deleteSurplusComa;

public class ColumnBuilder {
    private final StringBuilder query;
    private boolean isPreviousWasColumn;

    ColumnBuilder(StringBuilder query, boolean isPreviousWasColumn) {
        this.query = query;
        this.isPreviousWasColumn = isPreviousWasColumn;
    }

    public CaseBuilder caseStatement() {
        return new CaseBuilder(query.append(", "));
    }

    public ColumnBuilder column(String column) {
        if (isPreviousWasColumn) query.append(", ");

        query.append(column).append(" ");
        isPreviousWasColumn = true;
        return this;
    }

    public ColumnBuilder as(String column) {
        query.append("AS ").append(column).append(", ");
        isPreviousWasColumn = false;
        return this;
    }

    public JoinBuilder from(String table) {
        deleteSurplusComa(query);

        query.append("FROM ").append(table).append(" ");
        return new JoinBuilder(query);
    }

    public FunctionBuilder count(String column) {
        deleteSurplusComa(query);
        query.append(", COUNT");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder sum(String column) {
        deleteSurplusComa(query);
        query.append(", SUM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder avg(String column) {
        deleteSurplusComa(query);
        query.append(", AVG");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder min(String column) {
        deleteSurplusComa(query);
        query.append(", MIN");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder max(String column) {
        deleteSurplusComa(query);
        query.append(", MAX");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder upper(String column) {
        deleteSurplusComa(query);
        query.append(", UPPER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder lower(String column) {
        deleteSurplusComa(query);
        query.append(", LOWER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder concat(String... columns) {
        if (columns.length == 0) {
            throw new IllegalArgumentException("At least one column must be provided.");
        }

        deleteSurplusComa(query);
        query.append(", CONCAT(");

        if (columns.length == 1) {
            query.append(columns[0]);
        } else {
            query.append(String.join(", ", columns));
        }
        deleteSurplusComa(query);

        query.append(") ");
        return new FunctionBuilder(query);
    }

    public FunctionBuilder length(String column) {
        deleteSurplusComa(query);
        query.append(", LENGTH");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder trim(String column) {
        deleteSurplusComa(query);
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
