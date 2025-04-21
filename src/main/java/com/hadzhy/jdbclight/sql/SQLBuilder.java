package com.hadzhy.jdbclight.sql;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SQLBuilder {

    private SQLBuilder() {}

    public static SelectBuilder select() {
        return SelectBuilder.select();
    }

    public static SelectBuilder selectDistinct() {
        return SelectBuilder.selectDistinct();
    }

    public static SelectBuilder withAndSelect(String table, SQLQuery subQuery) {
        return SelectBuilder.with(table, subQuery);
    }

    public static InsertBuilder insert() {
        return new InsertBuilder();
    }

    public static String batchOf(SQLQuery... queries) {
        return Arrays.stream(queries)
                .map(SQLQuery::sql)
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .collect(Collectors.joining("; ")) + ";";
    }

    public static InsertBuilder withAndInsert(String table, SQLQuery subQuery) {
        return InsertBuilder.with(table, subQuery);
    }

    public static UpdateBuilder update(String table) {
        return UpdateBuilder.update(table);
    }

    public static DeleteBuilder delete() {
        return new DeleteBuilder();
    }
}