package io.github.hacihaciyev.jdbc;

import static java.util.Objects.requireNonNull;

public sealed interface FieldDecl {
    String column();
    Class<?> type();

    record Plain(String column, Class<?> type) implements FieldDecl {
        public Plain {
            requireNonNull(column, "column cannot be null");
            requireNonNull(type, "type cannot be null");
        }
    }

    record ValueObject(String column, Class<?> type) implements FieldDecl {
        public ValueObject {
            requireNonNull(column, "column cannot be null");
            requireNonNull(type, "type cannot be null");
        }
    }

    record Nested(String column, Class<?> type) implements FieldDecl {
        public Nested {
            requireNonNull(column, "column cannot be null");
            requireNonNull(type, "type cannot be null");
        }
    }
}