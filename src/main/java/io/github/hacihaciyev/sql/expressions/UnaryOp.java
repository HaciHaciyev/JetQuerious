package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public record UnaryOp(UnaryOperator operator, Expr expr) implements Expr {
    public UnaryOp {
        requireNonNull(operator);
        requireNonNull(expr);
    }
    
    public enum UnaryOperator {
        PLUS,
        MINUS,
        NOT
    }
}
