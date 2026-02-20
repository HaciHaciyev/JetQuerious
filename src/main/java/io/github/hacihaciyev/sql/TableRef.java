package io.github.hacihaciyev.sql;

import static java.util.Objects.requireNonNull;

public sealed interface TableRef {
    String name();
    
    sealed interface BaseTable extends TableRef permits Base, WithSchema, WithCatalog, WithCatalogAndSchema {}

    sealed interface SchemaRef permits WithSchema, WithCatalogAndSchema {
        String name();
        String schema();
    }

    sealed interface CatalogRef permits WithCatalog, WithCatalogAndSchema {
        String name();
        String catalog();
    }

    record Base(String name) implements BaseTable {
        public Base {
            name = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record WithSchema(String schema, String name) implements BaseTable, SchemaRef {
        public WithSchema {
            schema = requireNonNull(schema, "Schema cannot be null").trim();
            name   = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return schema + "." + name;
        }
    }

    record WithCatalog(String catalog, String name) implements BaseTable, CatalogRef {
        public WithCatalog {
            catalog = requireNonNull(catalog, "Catalog cannot be null").trim();
            name    = requireNonNull(name, "Table name cannot be null").trim();
        }

        @Override
        public String toString() {
            return catalog + "." + name;
        }
    }

    record WithCatalogAndSchema(String catalog, String schema, String name) implements BaseTable, SchemaRef, CatalogRef {
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
    
    record AliasedTable(BaseTable table, String alias) implements TableRef {
        public AliasedTable {
            requireNonNull(table, "Table cannot be null");
            alias   = requireNonNull(alias, "Alias cannot be null").trim();
        }
    
        @Override
        public String name() { 
            return table.name(); 
        }
    
        @Override
        public String toString() {
            return table + " AS " + alias;
        }
    }
}
