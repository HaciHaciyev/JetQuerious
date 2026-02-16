package io.github.hacihaciyev.sql.internal.schema;

import io.github.hacihaciyev.types.SQLType;

import static java.util.Objects.requireNonNull;

public sealed interface Column {
    String name();
    boolean known();
    boolean nullable();

    record Known(String name, SQLType type, boolean nullable) implements Column {
        public Known {
            requireNonNull(name, "Column name cannot be null");
            requireNonNull(type, "Type cannot be null");
            name = name.trim();
        }

        @Override
        public boolean known() {
            return true;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record Unknown(String name, boolean nullable) implements Column {
        public Unknown {
            requireNonNull(name, "Column name cannot be null");
            name = name.trim();
        }

        @Override
        public boolean known() {
            return false;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
