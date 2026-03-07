package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public sealed interface QuantifiedExpr extends Expr {
    
    record All(BinaryOp.BinaryOperator operator, Expr operand, Subquery.TableSubquery subquery) implements QuantifiedExpr { 
        public All {
            requireNonNull(operator);
            requireNonNull(operand);
            requireNonNull(subquery);
        }
    }
    
    record Any(BinaryOp.BinaryOperator operator, Expr operand, Subquery.TableSubquery subquery) implements QuantifiedExpr { 
        public Any {
            requireNonNull(operator);
            requireNonNull(operand);
            requireNonNull(subquery);
        }
    }
}