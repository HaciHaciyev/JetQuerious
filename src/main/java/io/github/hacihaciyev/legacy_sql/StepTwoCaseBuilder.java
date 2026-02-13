package io.github.hacihaciyev.sql;

public class StepTwoCaseBuilder {
    private final StringBuilder query;

    StepTwoCaseBuilder(StringBuilder query) {
        this.query = query.append("THEN ");
    }

    public StepThreeCaseBuilder then(String condition) {
        query.append(condition).append(" ");
        return new StepThreeCaseBuilder(query);
    }
}
