package io.github.hacihaciyev.schema;

import static java.util.Objects.requireNonNull;

public record UnknownBase(String name, boolean nullable) implements Column {
    public UnknownBase {
        requireNonNull(name, "Column name cannot be null");
    }

    @Override
    public boolean known() {
        return false;
    }
}
