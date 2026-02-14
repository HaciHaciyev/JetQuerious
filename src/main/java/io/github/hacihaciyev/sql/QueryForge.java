package io.github.hacihaciyev.sql;

import java.util.Arrays;
import java.util.stream.Collectors;

public class QueryForge {

    private QueryForge() {}

    public static SelectBuilder select() {
        return SelectBuilder.select();
    }

    public static SelectBuilder selectDistinct() {
        return SelectBuilder.selectDistinct();
    }

    public static SelectBuilder withAndSelect(String table, JQ subQuery) {
        return SelectBuilder.with(table, subQuery);
    }

    public static InsertBuilder insert() {
        return new InsertBuilder();
    }

    public static String batchOf(JQ... queries) {
        return Arrays.stream(queries)
                .map(JQ::sql)
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .collect(Collectors.joining("; ")) + ";";
    }

    public static InsertBuilder withAndInsert(String table, JQ subQuery) {
        return InsertBuilder.with(table, subQuery);
    }

    public static UpdateBuilder update(String table) {
        return new UpdateBuilder(table);
    }

    public static DeleteBuilder delete() {
        return new DeleteBuilder();
    }
}