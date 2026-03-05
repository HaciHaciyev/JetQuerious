package io.github.hacihaciyev.sql.expressions;

import io.github.hacihaciyev.sql.JQ;

import static java.util.Objects.requireNonNull;

public record Exists(JQ subquery) implements Expr {
    public Exists {
        requireNonNull(subquery);
    }
}