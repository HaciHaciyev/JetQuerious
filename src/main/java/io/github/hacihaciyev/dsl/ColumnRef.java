package io.github.hacihaciyev.dsl;

import static java.util.Objects.requireNonNull;

public sealed interface ColumnRef {
    String name();

    record Base(String name) implements ColumnRef {
        public Base {
            name = requireNonNull(name, "Column reference cannot be null").trim();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record Alias(String name, String alias) implements ColumnRef {
        public Alias {
            name = requireNonNull(name, "Column reference cannot be null").trim();
            alias = requireNonNull(alias, "Column alias cannot be null").trim();
        }

        @Override
        public String toString() {
            return name + " AS " + alias;
        }
    }

    record VariableBase(String variable, String name) implements ColumnRef {
        public VariableBase {
            variable = requireNonNull(variable, "Column variable cannot be null").trim();
            name = requireNonNull(name, "Column reference cannot be null").trim();
        }

        @Override
        public String toString() {
            return variable + "." + name;
        }
    }

    record VariableAlias(String variable, String name, String alias) implements ColumnRef {
        public VariableAlias {
            variable = requireNonNull(variable, "Column variable cannot be null").trim();
            name = requireNonNull(name, "Column alias cannot be null").trim();
            alias = requireNonNull(alias, "Column alias cannot be null").trim();
        }

        @Override
        public String toString() {
            return variable + "." + name + " AS " + alias;
        }
    }
}
