package io.github.hacihaciyev.sql;

public class CaseBuilder {
    private final StringBuilder query;

    CaseBuilder(StringBuilder query) {
        this.query = query.append("CASE ");
    }

    public StepTwoCaseBuilder when(String condition) {
        query.append("WHEN ").append(condition).append(" ");
        return new StepTwoCaseBuilder(query);
    }
}
