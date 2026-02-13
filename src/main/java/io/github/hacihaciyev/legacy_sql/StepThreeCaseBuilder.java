package io.github.hacihaciyev.sql;

public class StepThreeCaseBuilder {
    private final StringBuilder query;

    StepThreeCaseBuilder(StringBuilder query) {
        this.query = query;
    }

    public StepTwoCaseBuilder when(String condition) {
        query.append("WHEN ").append(condition).append(" ");
        return new StepTwoCaseBuilder(query);
    }

    public ElseCaseBuilder elseCase(String elseCase) {
        query.append("ELSE ").append(elseCase).append(" ");
        return new ElseCaseBuilder(query);
    }

    public FromBuilder end() {
        query.append("END ");
        return new FromBuilder(query);
    }

    public FromBuilder endAs(String alias) {
        query.append("END AS ").append(alias).append(" ");
        return new FromBuilder(query);
    }
}
