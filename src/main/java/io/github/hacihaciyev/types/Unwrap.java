package io.github.hacihaciyev.types;

import static java.util.Objects.requireNonNull;

public record Unwrap(Record value) {
    public Unwrap {
        requireNonNull(value, "Null is not allowed for tuples");
    }
}
