package io.github.hacihaciyev.sql.expressions;

import static java.util.Objects.requireNonNull;

public sealed interface Literal extends Expr {
   
    record StringLiteral(String value) implements Literal {
        public StringLiteral {
            requireNonNull(value);
        }
    }
    
    record IntLiteral(int value) implements Literal {}
    
    record LongLiteral(long value) implements Literal {}
    
    record BooleanLiteral(boolean value) implements Literal {}
    
    record NullLiteral() implements Literal {}
    
    record GenericLiteral(Object value) implements Literal {
        public GenericLiteral {
            requireNonNull(value);
        }
    }
}