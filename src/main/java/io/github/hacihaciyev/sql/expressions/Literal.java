package io.github.hacihaciyev.sql.expressions;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public sealed interface Literal extends ValueExpr {
    
    sealed interface ValueLiteral permits StringLiteral, IntLiteral, LongLiteral, BooleanLiteral,
            NullLiteral, FloatLiteral, DoubleLiteral, BigDecimalLiteral, GenericLiteral {}
   
    record StringLiteral(String value) implements Literal, ValueLiteral {
        public StringLiteral {
            requireNonNull(value);
        }
    }
    
    record IntLiteral(int value) implements Literal, ValueLiteral {}
    
    record LongLiteral(long value) implements Literal, ValueLiteral {}
    
    record BooleanLiteral(boolean value) implements Literal, ValueLiteral {}
    
    record NullLiteral() implements Literal, ValueLiteral {}

    record FloatLiteral(float value) implements Literal, ValueLiteral {}
    
    record DoubleLiteral(double value) implements Literal, ValueLiteral {}
    
    record BigDecimalLiteral(BigDecimal value) implements Literal, ValueLiteral { 
        public BigDecimalLiteral {
            requireNonNull(value);
        }
    }
    
    record GenericLiteral(Object value) implements Literal, ValueLiteral {
        public GenericLiteral {
            requireNonNull(value);
        }
    }
    
    record PlaceholderLiteral(Class<?> type) implements Literal {
        public PlaceholderLiteral {
            requireNonNull(type);
        }
    }
}