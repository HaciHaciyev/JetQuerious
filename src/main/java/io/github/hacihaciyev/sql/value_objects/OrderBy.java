package io.github.hacihaciyev.sql.value_objects;

import static java.util.Objects.requireNonNull;

import io.github.hacihaciyev.sql.expressions.ColumnRef;

public sealed interface OrderBy {
    ASC ASC = new ASC();
    DESC DESC = new DESC();
    
    record ASC(ColumnRef... columns) implements OrderBy {
        public ASC {
            requireNonNull(columns, "Columns cannot be null");
            for (var c : columns) requireNonNull(c, "Columns cannot be null");
        }
        
        public ColumnRef[] columns() {
            return columns.clone();
        }
    }
    
    record DESC(ColumnRef... columns) implements OrderBy {
        public DESC {
            requireNonNull(columns, "Columns cannot be null");
            for (var c : columns) requireNonNull(c, "Columns cannot be null");
        }
        
        public ColumnRef[] columns() {
            return columns.clone();
        }
    }
    
}