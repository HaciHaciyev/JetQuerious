package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.types.SQLType;

import static java.util.Objects.requireNonNull;

public sealed interface ColumnType {
    String name();
    boolean isNullable();

    record Known(SQLType type, boolean isNullable) implements ColumnType {
        public Known {
            requireNonNull(type, "Type can`t be null");
        }

        @Override
        public String name() {
            return type.name();
        }
    }

    record Unknown(String name, boolean isNullable) implements ColumnType {
        public Unknown {
            requireNonNull(name, "Name can`t be null");
        }
    }
}
