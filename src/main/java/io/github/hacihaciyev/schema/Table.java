package io.github.hacihaciyev.schema;

import java.util.Optional;
import static java.util.Objects.requireNonNull;

public record Table(String name, Column[] columns) {

    public Table {
        requireNonNull(name, "Table name cannot be null");
        requireNonNull(columns, "Columns array cannot be null");
        columns = columns.clone();
    }

    public Optional<Column> column(String name) {
        requireNonNull(name, "Column name cannot be null");

        for (Column c : columns) {
            if (c.name().equals(name)) return Optional.of(c);
        }
        return Optional.empty();
    }

    public boolean hasColumn(String name) {
        requireNonNull(name, "Column name cannot be null");

        for (Column c : columns) {
            if (c.name().equals(name)) return true;
        }
        return false;
    }

    public Column[] columns() {
        return columns.clone();
    }
}
