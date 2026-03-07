package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public sealed interface BetweenExpr extends Expr  {
    
    record Between(Expr operand, Expr low, Expr high) implements BetweenExpr { 
        public Between {
            requireNonNull(operand);
            requireNonNull(low);
            requireNonNull(high);
        }
    }
    
    record NotBetween(Expr operand, Expr low, Expr high) implements BetweenExpr { 
        public NotBetween {
            requireNonNull(operand);
            requireNonNull(low);
            requireNonNull(high);
        }
    }
}