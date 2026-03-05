package io.github.hacihaciyev.sql.expressions;

import io.github.hacihaciyev.sql.JQ;
import static java.util.Objects.requireNonNull;

public record Subquery(JQ jq) implements Expr {
    public Subquery {
        requireNonNull(jq);
    }
}