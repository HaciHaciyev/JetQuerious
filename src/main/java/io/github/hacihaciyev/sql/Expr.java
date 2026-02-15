package io.github.hacihaciyev.sql;

public sealed interface Expr {

    All ALL = new All();
    
    record Col(ColumnRef col) implements Expr {
        public Col(String col) {
            this(new ColumnRef.Base(col)));
        }
    }
    
    record All() implements Expr {
        static final String value = "*";
    }
}