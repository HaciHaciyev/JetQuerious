package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public record Exists(Subquery.TableSubquery subquery) implements ValueExpr {
    public Exists {
        requireNonNull(subquery);
    }
}