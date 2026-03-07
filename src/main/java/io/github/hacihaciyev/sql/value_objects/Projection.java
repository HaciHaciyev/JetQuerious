package io.github.hacihaciyev.sql.value_objects;

import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.expressions.ValueExpr;

import static java.util.Objects.requireNonNull;

public sealed interface Projection {
    
    record Base(Expr expr) implements Projection {
        public Base {
            requireNonNull(expr);
        }
    }
    
    record Aliased(ValueExpr expr, String alias) implements Projection {
        public Aliased {
            requireNonNull(expr);
            requireNonNull(alias);
            if (alias.isBlank()) throw new IllegalArgumentException("alias cannot be empty");
        }
    }
    
    record Wildcard() implements Projection {}
    
    record QualifiedWildcard(String qualifier) implements Projection {
        public QualifiedWildcard {
            requireNonNull(qualifier);
        }
    }
}