package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.types.SQLType;

import static java.util.Objects.requireNonNull;

public record KnownAlias(String alias, String name, SQLType type, boolean nullable) implements Column {
    public KnownAlias {
        requireNonNull(alias, "Alias cannot be null");
        requireNonNull(name, "Column name cannot be null");
        requireNonNull(type, "Type cannot be null");
        alias = alias.trim();
        name = name.trim();
    }

    @Override
    public boolean known() {
        return true;
    }

    @Override
    public String toString() {
        return alias + "." + name;
    }
}
