package io.github.hacihaciyev.sql.internal.schema;

import io.github.hacihaciyev.sql.ColumnRef;
import io.github.hacihaciyev.sql.TableRef;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record Table(Catalog catalog, Schema schema, String name, Column[] columns) {
    public Table {
        requireNonNull(catalog, "Catalog cannot be null");
        requireNonNull(schema, "Schema cannot be null");
        requireNonNull(name, "Table name cannot be null");
        requireNonNull(columns, "Columns cannot be null");
        if (columns.length == 0) throw new IllegalArgumentException("Table cannot have zero columns");
    }

    public sealed interface Catalog {
        record Known(String name) implements Catalog {
            public Known {
                name = requireNonNull(name, "Catalog name cannot be null").trim();
            }

            @Override
            public String toString() {
                return name;
            }
        }

        record Unknown() implements Catalog {}
    }

    public sealed interface Schema {
        record Known(String name) implements Schema {
            public Known {
                name = requireNonNull(name, "Schema name cannot be null").trim();
            }

            @Override
            public String toString() {
                return name;
            }
        }

        record Unknown() implements Schema {}
    }

    public Optional<Column> column(ColumnRef column, TableRef table) {
        requireNonNull(column, "Column cannot be null");
        requireNonNull(table, "Table reference cannot be null");

        if (!tableMatch(table)) return Optional.empty();

        for (Column c : columns) {
            if (c.name().equalsIgnoreCase(column.name())) return Optional.of(c);
        }
        return Optional.empty();
    }

    public boolean hasColumn(ColumnRef column, TableRef table) {
        requireNonNull(column, "Column cannot be null");
        requireNonNull(table, "Table reference cannot be null");

        if (!tableMatch(table)) return false;

        for (Column c : columns) {
            if (c.name().equalsIgnoreCase(column.name())) return true;
        }
        return false;
    }

    private boolean tableMatch(TableRef table) {
        return switch (table) {
            case TableRef.Base(var name) -> eqTableName(name);
            case TableRef.WithSchema(var schem, var name) -> eqTableName(name) && eqSchem(schem);
            case TableRef.WithCatalog(var cat, var name) -> eqTableName(name) && eqCat(cat);
            case TableRef.WithCatalogAndSchema(var cat, var schem, var name) -> eqTableName(name) && eqCat(cat) && eqSchem(schem);
            case TableRef.AliasedTable(var at, _) -> tableMatch(at);
        };
    }

    private boolean eqTableName(String other) {
        return name.equalsIgnoreCase(other);
    }

    private boolean eqSchem(String other) {
        if (schema instanceof Schema.Known(var known)) return known.equalsIgnoreCase(other);
        return false;
    }

    private boolean eqCat(String other) {
        if (catalog instanceof Catalog.Known(var known)) return known.equalsIgnoreCase(other);
        return false;
    }
}
