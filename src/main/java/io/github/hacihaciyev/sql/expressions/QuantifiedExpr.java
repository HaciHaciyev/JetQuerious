package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public sealed interface QuantifiedExpr extends Expr {

    enum ComparisonOperator {
        EQ, NEQ, GT, GTE, LT, LTE
    }
    
    record All(ComparisonOperator operator, Expr operand, Subquery.TableSubquery subquery) implements QuantifiedExpr { 
        public All {
            requireNonNull(operator);
            requireNonNull(operand);
            requireNonNull(subquery);
        }
    }
    
    record Any(ComparisonOperator operator, Expr operand, Subquery.TableSubquery subquery) implements QuantifiedExpr { 
        public Any {
            requireNonNull(operator);
            requireNonNull(operand);
            requireNonNull(subquery);
        }
    }
}