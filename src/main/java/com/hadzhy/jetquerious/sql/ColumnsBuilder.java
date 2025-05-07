package com.hadzhy.jetquerious.sql;

import java.util.Arrays;
import java.util.List;

import static com.hadzhy.jetquerious.sql.Util.deleteSurplusComa;

public class ColumnsBuilder {
    private final StringBuilder query;
    private final List<String> columnsList;

    ColumnsBuilder(StringBuilder query, List<String> columnsList) {
        this.query = query.append("(");
        this.columnsList = columnsList;
    }

    public ValuesBuilder columns(String... columns) {
        columnsList.addAll(Arrays.asList(columns));
        query.append(String.join(", ", columns));
        return new ValuesBuilder(query, columnsList);
    }

    public ColumnsBuilder column(String column) {
        columnsList.add(column);
        query.append(column).append(", ");
        return this;
    }

    public TailInsertBuilder values() {
        int countOfValues = columnsList.size();
        if (countOfValues == 0) throw new IllegalArgumentException("Values can`t be 0.");
        deleteSurplusComa(query);

        query.append(") ").append("VALUES ").append("(");
        for (int i = 0; i < countOfValues; i++) {
            query.append("?");
            if (i < countOfValues - 1) query.append(", ");
        }
        query.append(") ");
        return new TailInsertBuilder(query);
    }
}
