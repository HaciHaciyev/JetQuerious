package io.github.hacihaciyev.sql;

import static java.util.Objects.requireNonNull;

public sealed interface ColumnRef {

    String name();
    
    sealed interface BaseColumn extends ColumnRef permits Base, Alias, VariableColumn {}
    
    sealed interface VariableColumn extends BaseColumn permits VariableBase, VariableAlias {
        String variable();
    }

    record Base(String name) implements BaseColumn {
        public Base {
            name = validate(name, "column");
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record Alias(String name, String alias) implements BaseColumn {
        public Alias {
            name = validate(name, "column");
            alias = validate(alias, "alias");
        }

        @Override
        public String toString() {
            return name + " AS " + alias;
        }
    }

    record VariableBase(String variable, String name) implements VariableColumn {
        public VariableBase {
            variable = validate(variable, "variable");
            name = validate(name, "column");
        }

        @Override
        public String toString() {
            return variable + "." + name;
        }
    }

    record VariableAlias(String variable, String name, String alias) implements VariableColumn {
        public VariableAlias {
            variable = validate(variable, "variable");
            name = validate(name, "column");
            alias = validate(alias, "alias");
        }

        @Override
        public String toString() {
            return variable + "." + name + " AS " + alias;
        }
    }
    
    record Typed(BaseColumn column, Class<?> type) implements ColumnRef {
        public Typed {
            requireNonNull(column, "Column cannot be null for ColumnRef.Typed");
            requireNonNull(type, "Type cannot be null for ColumnRef.Typed");
        }
        
        @Override
        public String name() {
            return column.name();
        }
        
        @Override
        public String toString() {
            return column.toString();
        }
    }

    private static String validate(String value, String label) {
        String res = requireNonNull(value, label + " cannot be null").trim();
        if (res.isEmpty()) throw new IllegalArgumentException(label + " cannot be empty");
        return res;
    }
}