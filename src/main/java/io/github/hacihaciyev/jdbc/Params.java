package io.github.hacihaciyev.jdbc;

import static java.util.Objects.requireNonNull;

public sealed interface Params {

    record Value(Object value) implements Params {
        public Value {
            requireNonNull(value, "value must not be null");
        }
    }

    record Dec(Deconstruction value) implements Params {
        public Dec {
            requireNonNull(value, "value must not be null");
        }
    }
}