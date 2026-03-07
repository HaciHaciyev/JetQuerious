package io.github.hacihaciyev.sql.expressions;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public sealed interface Literal extends ValueExpr {
   
    record StringLiteral(String value) implements Literal {
        public StringLiteral {
            requireNonNull(value);
        }
    }
    
    record IntLiteral(int value) implements Literal {}
    
    record LongLiteral(long value) implements Literal {}
    
    record BooleanLiteral(boolean value) implements Literal {}
    
    record NullLiteral() implements Literal {}

    record FloatLiteral(float value) implements Literal {}
    
    record DoubleLiteral(double value) implements Literal {}
    
    record BigDecimalLiteral(BigDecimal value) implements Literal { 
        public BigDecimalLiteral {
            requireNonNull(value);
        }
    }
    
    record GenericLiteral(Object value) implements Literal {
        public GenericLiteral {
            requireNonNull(value);
        }
    }
}