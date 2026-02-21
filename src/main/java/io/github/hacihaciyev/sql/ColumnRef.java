package io.github.hacihaciyev.sql;

import static java.util.Objects.requireNonNull;

public sealed interface ColumnRef {

    String name();
    Type type();
    
    sealed interface Type {
        None NONE = new None();
        
        record Some(Class<?> type) implements Type {
            public Some {
                requireNonNull(type, "Type cannot be null");
            }
        }
        
        record None() implements Type {}
    }
        
    sealed interface VariableColumn permits VariableBase, VariableAlias {
        String variable();
    }

    record Base(String name, Type type) implements ColumnRef {
        public Base {
            name = validate(name, "column");
            requireNonNull(type, "Type cannot be null");
        }
        
        public Base(String name) {
            this(name, Type.NONE);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record Alias(String name, String alias, Type type) implements ColumnRef {
        public Alias {
            name = validate(name, "column");
            alias = validate(alias, "alias");
            requireNonNull(type, "Type cannot be null");
        }
        
        public Alias(String name, String alias) {
            this(name, alias, Type.NONE);
        }

        @Override
        public String toString() {
            return name + " AS " + alias;
        }
    }

    record VariableBase(String variable, String name, Type type) implements ColumnRef, VariableColumn {
        public VariableBase {
            variable = validate(variable, "variable");
            name = validate(name, "column");
            requireNonNull(type, "Type cannot be null");
        }
        
        public VariableBase(String variable, String name) {
            this(variable, name, Type.NONE);
        }

        @Override
        public String toString() {
            return variable + "." + name;
        }
    }

    record VariableAlias(String variable, String name, String alias, Type type) implements ColumnRef, VariableColumn {
        public VariableAlias {
            variable = validate(variable, "variable");
            name = validate(name, "column");
            alias = validate(alias, "alias");
            requireNonNull(type, "Type cannot be null");
        }
        
        public VariableAlias(String variable, String name, String alias) {
            this(variable, name, alias, Type.NONE);
        }

        @Override
        public String toString() {
            return variable + "." + name + " AS " + alias;
        }
    }
    
    private static String validate(String value, String label) {
        String res = requireNonNull(value, label + " cannot be null").trim();
        if (res.isEmpty()) throw new IllegalArgumentException(label + " cannot be empty");
        return res;
    }
}