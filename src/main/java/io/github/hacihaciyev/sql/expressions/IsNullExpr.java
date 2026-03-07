package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public sealed interface IsNullExpr extends Expr {
    
    record IsNull(Expr operand) implements IsNullExpr { 
        public IsNull {
            requireNonNull(operand);
        }
    }
    
    record IsNotNull(Expr operand) implements IsNullExpr { 
        public IsNotNull {
            requireNonNull(operand);
        }
    }
}