package io.github.hacihaciyev.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    public List<Column> byAlias(String alias) {
        requireNonNull(alias, "Alias cannot be null");
        List<Column> result = new ArrayList<>();

        for (Column c : columns) {
            switch (c) {
                case KnownAlias ka when ka.alias().equals(alias) -> result.add(c);
                case UnknownAlias ua when ua.alias().equals(alias) -> result.add(c);
                default -> {}
            }
        }

        return Collections.unmodifiableList(result);
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
