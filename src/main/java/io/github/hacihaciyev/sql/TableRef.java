package io.github.hacihaciyev.dsl;

import static java.util.Objects.requireNonNull;

public sealed interface TableRef {
    String name();

    sealed interface SchemaRef permits WithSchema, WithCatalogAndSchema {
        String schema();
    }

    sealed interface CatalogRef permits WithCatalog, WithCatalogAndSchema {
        String catalog();
    }

    record Base(String name) implements TableRef {
        public Base {
            name = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record WithSchema(String schema, String name) implements TableRef, SchemaRef {
        public WithSchema {
            schema = requireNonNull(schema, "Schema cannot be null").trim();
            name   = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return schema + "." + name;
        }
    }

    record WithCatalog(String catalog, String name) implements TableRef, CatalogRef {
        public WithCatalog {
            catalog = requireNonNull(catalog, "Catalog cannot be null").trim();
            name    = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return catalog + "." + name;
        }
    }

    record WithCatalogAndSchema(String catalog, String schema, String name) implements TableRef, SchemaRef, CatalogRef {
        public WithCatalogAndSchema {
            catalog = requireNonNull(catalog, "Catalog cannot be null").trim();
            schema  = requireNonNull(schema, "Schema cannot be null").trim();
            name    = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return catalog + "." + schema + "." + name;
        }
    }
}
