package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

import io.github.hacihaciyev.sql.JQ;

public record Exists(JQ subquery) implements Expr {
    public Exists {
        requireNonNull(subquery);
    }
}