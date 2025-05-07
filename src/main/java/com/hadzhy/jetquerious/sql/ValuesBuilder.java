package com.hadzhy.jetquerious.sql;

import java.util.List;

public class ValuesBuilder {
    private final StringBuilder query;
    private final List<String> columnsList;

    ValuesBuilder(StringBuilder query, List<String> columnsList) {
        this.query = query.append(") ");
        this.columnsList = columnsList;
    }

    public SelectBuilder select() {
        return SelectBuilder.select(query);
    }

    public SelectBuilder selectDistinct() {
        return SelectBuilder.selectDistinct(query);
    }

    public SelectBuilder withAndSelect(String table, SQLQuery subQuery) {
        return SelectBuilder.with(query, table, subQuery);
    }

    public TailInsertBuilder values() {
        int countOfValues = columnsList.size();
        if (countOfValues == 0) {
            throw new IllegalArgumentException("Values can`t be 0.");
        }

        query.append("VALUES ").append("(");
        for (int i = 0; i < countOfValues; i++) {
            query.append("?");
            if (i < countOfValues - 1) {
                query.append(", ");
            }
        }
        query.append(") ");
        return new TailInsertBuilder(query);
    }
}
