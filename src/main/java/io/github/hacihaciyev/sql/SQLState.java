package io.github.hacihaciyev.sql;

public class SQLState {
    private final String sql;

    SQLState(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }

    public SQLQuery toSQlQuery() {
        return new SQLQuery(sql);
    }
}
