package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

import io.github.hacihaciyev.sql.JQ;

public record Subquery(JQ jq) implements Expr {
    public Subquery {
        requireNonNull(jq);
    }
}