package com.hadzhy.jdbclight.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InsertBuilder {
    private final StringBuilder query;
    private final List<String> columnsList = new ArrayList<>();

    InsertBuilder() {
        this.query = new StringBuilder();
    }

    private InsertBuilder(StringBuilder query) {
        this.query = query;
    }

    static InsertBuilder with(String table, SQLQuery subQuery) {
        return new InsertBuilder(new StringBuilder("WITH ").append(table).append(" AS ").append("(").append(subQuery.sql()).append(") "));
    }

    public ValuesBuilder into(String table, String... columns) {
        columnsList.addAll(Arrays.asList(columns));
        query.append("INSERT INTO ").append(table).append(" (").append(String.join(", ", columns));
        return new ValuesBuilder(query, columnsList);
    }

    public ColumnsBuilder into(String table) {
        return new ColumnsBuilder(query.append("INSERT INTO ").append(table).append(" "), columnsList);
    }

    public TailInsertBuilder defaultValues(String table) {
        query.append("INSERT INTO ").append(table).append(" DEFAULT VALUES ");
        return new TailInsertBuilder(query);
    }

    public TailInsertBuilder defaultValues(String table, String... columns) {
        query.append("INSERT INTO ").append(table).append(" (").append(String.join(", ", columns)).append(") DEFAULT VALUES ");
        return new TailInsertBuilder(query);
    }
}
