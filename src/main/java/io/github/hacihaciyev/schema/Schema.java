package io.github.hacihaciyev.schema;

import java.util.Optional;
import static java.util.Objects.requireNonNull;

public record Schema(Table[] tables) {

    public Schema {
        requireNonNull(tables, "Tables array cannot be null");
        tables = tables.clone();
    }

    public Optional<Table> table(String name) {
        requireNonNull(name, "Table name cannot be null");

        for (Table t : tables) {
            if (t.name().equals(name)) return Optional.of(t);
        }
        return Optional.empty();
    }

    public boolean hasTable(String name) {
        requireNonNull(name, "Table name cannot be null");

        for (Table t : tables) {
            if (t.name().equals(name)) return true;
        }
        return false;
    }

    public Table[] tables() {
        return tables.clone();
    }
}
