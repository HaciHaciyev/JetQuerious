package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public record BinaryOp(BinaryOperator operator, Expr left, Expr right) implements Expr {
    public BinaryOp {
        requireNonNull(operator);
        requireNonNull(left);
        requireNonNull(right);
    }
    
    public enum BinaryOperator {
        PLUS,
        MINUS,
        MULTIPLY,
        DIV,
        AND,
        OR,
        EQ,
        GT,
        LT
    }
}