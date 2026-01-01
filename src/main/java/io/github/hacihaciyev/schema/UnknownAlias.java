package io.github.hacihaciyev.schema;

import static java.util.Objects.requireNonNull;

public record UnknownAlias(String alias, String name, boolean nullable) implements Column {
    public UnknownAlias {
        requireNonNull(alias, "Alias cannot be null");
        requireNonNull(name, "Column name cannot be null");
    }

    @Override
    public boolean known() {
        return false;
    }
}
