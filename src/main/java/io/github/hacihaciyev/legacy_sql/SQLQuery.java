package io.github.hacihaciyev.sql;

public class SQLQuery {
    private final String sql;

    SQLQuery(String sql) {
        if (sql == null) throw new IllegalArgumentException("SQL can`t be null");
        if (sql.isBlank()) throw new IllegalArgumentException("SQL can`t be blank");

        this.sql = sql;
    }

    public String sql() {
        return sql;
    }

    public static SQLQuery of(String query) {
        return new SQLQuery(query);
    }
}
