package io.github.hacihaciyev.sql;

import static java.util.Objects.requireNonNull;

public sealed interface TableRef {
    
    String name();
    
    sealed interface Aliased permits AliasedBase, AliasedWithSchema, AliasedWithCatalog, AliasedWithCatalogAndSchema {
        String alias();
    }
    
    sealed interface SchemaRef permits WithSchema, WithCatalogAndSchema, AliasedWithSchema, AliasedWithCatalogAndSchema {
        String schema();
    }
    
    sealed interface CatalogRef permits WithCatalog, WithCatalogAndSchema, AliasedWithCatalog, AliasedWithCatalogAndSchema {
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
    
    record AliasedBase(String name, String alias) implements TableRef, Aliased {
        public AliasedBase {
            name  = requireNonNull(name, "Table name cannot be null").trim();
            alias = requireNonNull(alias, "Alias cannot be null").trim();
        }
        
        @Override
        public String toString() {
            return name + " AS " + alias;
        }
    }
    
    record AliasedWithSchema(String schema, String name, String alias) implements TableRef, Aliased, SchemaRef {
        public AliasedWithSchema {
            schema = requireNonNull(schema, "Schema cannot be null").trim();
            name   = requireNonNull(name, "Table name cannot be null").trim();
            alias  = requireNonNull(alias, "Alias cannot be null").trim();
        }
        
        @Override
        public String toString() {
            return schema + "." + name + " AS " + alias;
        }
    }
    
    record AliasedWithCatalog(String catalog, String name, String alias) implements TableRef, Aliased, CatalogRef {
        public AliasedWithCatalog {
            catalog = requireNonNull(catalog, "Catalog cannot be null").trim();
            name    = requireNonNull(name, "Table name cannot be null").trim();
            alias   = requireNonNull(alias, "Alias cannot be null").trim();
        }
        
        @Override
        public String toString() {
            return catalog + "." + name + " AS " + alias;
        }
    }
    
    record AliasedWithCatalogAndSchema(String catalog, String schema, String name, String alias) implements TableRef, Aliased, SchemaRef, CatalogRef {
        public AliasedWithCatalogAndSchema {
            catalog = requireNonNull(catalog, "Catalog cannot be null").trim();
            schema  = requireNonNull(schema, "Schema cannot be null").trim();
            name    = requireNonNull(name, "Table name cannot be null").trim();
            alias   = requireNonNull(alias, "Alias cannot be null").trim();
        }
        
        @Override
        public String toString() {
            return catalog + "." + schema + "." + name + " AS " + alias;
        }
    }
}