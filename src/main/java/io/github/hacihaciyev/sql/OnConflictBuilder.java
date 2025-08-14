package io.github.hacihaciyev.sql;

public class OnConflictBuilder {
    private final StringBuilder query;

    OnConflictBuilder(StringBuilder query) {
        this.query = query;
    }

    public ReturningBuilder doUpdateSet(String doUpdateSet) {
        return new ReturningBuilder(query.append("DO UPDATE SET ").append(doUpdateSet).append(" "));
    }

    public ReturningBuilder doNothing() {
        return new ReturningBuilder(query.append("DO NOTHING "));
    }
}
