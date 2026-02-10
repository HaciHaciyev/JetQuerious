package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.dsl.ColumnRef;
import io.github.hacihaciyev.dsl.TableRef;
import io.github.hacihaciyev.types.SQLType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @Test
    @DisplayName("Should create table with known catalog and schema")
    void shouldCreateTableWithKnownCatalogAndSchema() {
        var table = new Table(
                new Table.Catalog.Known("my_catalog"),
                new Table.Schema.Known("my_schema"),
                "users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );

        assertEquals("users", table.name());
        assertInstanceOf(Table.Catalog.Known.class, table.catalog());
        assertInstanceOf(Table.Schema.Known.class, table.schema());
    }

    @Test
    @DisplayName("Should create table with unknown catalog and schema")
    void shouldCreateTableWithUnknownCatalogAndSchema() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Unknown(),
                "products",
                new Column[]{new Column.Known("id", SQLType.BIGINT, false)}
        );

        assertInstanceOf(Table.Catalog.Unknown.class, table.catalog());
        assertInstanceOf(Table.Schema.Unknown.class, table.schema());
    }

    @Test
    @DisplayName("Should throw exception when columns array is empty")
    void shouldThrowExceptionWhenColumnsIsEmpty() {
        var exception = assertThrows(IllegalArgumentException.class, () ->
                new Table(
                        new Table.Catalog.Known("catalog"),
                        new Table.Schema.Known("schema"),
                        "table",
                        new Column[]{}
                )
        );
        assertTrue(exception.getMessage().contains("cannot have zero columns"));
    }

    @Test
    @DisplayName("Should find column by name case-insensitively")
    void shouldFindColumnCaseInsensitively() {
        var table = createSampleTable();
        var tableRef = new TableRef.Base("users");

        assertTrue(table.column(new ColumnRef.Base("id"), tableRef).isPresent());
        assertTrue(table.column(new ColumnRef.Base("ID"), tableRef).isPresent());
        assertTrue(table.column(new ColumnRef.Base("name"), tableRef).isPresent());
        assertTrue(table.column(new ColumnRef.Base("NAME"), tableRef).isPresent());
    }

    @Test
    @DisplayName("Should return empty for non-existent column")
    void shouldReturnEmptyForNonExistentColumn() {
        var table = createSampleTable();
        var tableRef = new TableRef.Base("users");

        assertTrue(table.column(new ColumnRef.Base("non_existent"), tableRef).isEmpty());
    }

    @Test
    @DisplayName("Should return empty when table reference doesn't match")
    void shouldReturnEmptyWhenTableRefDoesntMatch() {
        var table = createSampleTable();
        var wrongTableRef = new TableRef.Base("products");

        assertTrue(table.column(new ColumnRef.Base("id"), wrongTableRef).isEmpty());
    }

    @Test
    @DisplayName("Should find column with matching schema")
    void shouldFindColumnWithMatchingSchema() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Known("public"),
                "users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );
        var tableRef = new TableRef.WithSchema("public", "users");

        var result = table.column(new ColumnRef.Base("id"), tableRef);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should not find column when schema doesn't match")
    void shouldNotFindColumnWhenSchemaDoesntMatch() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Known("public"),
                "users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );
        var tableRef = new TableRef.WithSchema("private", "users");

        assertTrue(table.column(new ColumnRef.Base("id"), tableRef).isEmpty());
    }

    @Test
    @DisplayName("Should find column with matching catalog and schema")
    void shouldFindColumnWithMatchingCatalogAndSchema() {
        var table = new Table(
                new Table.Catalog.Known("my_db"),
                new Table.Schema.Known("public"),
                "users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );
        var tableRef = new TableRef.WithCatalogAndSchema("my_db", "public", "users");

        assertTrue(table.column(new ColumnRef.Base("id"), tableRef).isPresent());
    }

    @Test
    @DisplayName("Should match table name case-insensitively")
    void shouldMatchTableNameCaseInsensitively() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Unknown(),
                "Users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );

        assertTrue(table.hasColumn(new ColumnRef.Base("id"), new TableRef.Base("users")));
        assertTrue(table.hasColumn(new ColumnRef.Base("id"), new TableRef.Base("USERS")));
    }

    @Test
    @DisplayName("Should match schema name case-insensitively")
    void shouldMatchSchemaNameCaseInsensitively() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Known("Public"),
                "users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );

        assertTrue(table.hasColumn(new ColumnRef.Base("id"), new TableRef.WithSchema("public", "users")));
        assertTrue(table.hasColumn(new ColumnRef.Base("id"), new TableRef.WithSchema("PUBLIC", "users")));
    }

    @Test
    @DisplayName("Should not match when table has unknown schema but ref specifies schema")
    void shouldNotMatchWhenTableHasUnknownSchemaButRefSpecifies() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Unknown(),
                "users",
                new Column[]{new Column.Known("id", SQLType.INTEGER, false)}
        );

        assertFalse(table.hasColumn(new ColumnRef.Base("id"), new TableRef.WithSchema("public", "users")));
    }

    @Test
    @DisplayName("Should preserve column type and nullable information")
    void shouldPreserveColumnTypeAndNullable() {
        var table = new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Unknown(),
                "users",
                new Column[]{
                        new Column.Known("id", SQLType.INTEGER, false),
                        new Column.Known("name", SQLType.VARCHAR, true),
                        new Column.Unknown("metadata", true)
                }
        );

        var idCol = table.column(new ColumnRef.Base("id"), new TableRef.Base("users")).orElseThrow();
        var nameCol = table.column(new ColumnRef.Base("name"), new TableRef.Base("users")).orElseThrow();
        var metaCol = table.column(new ColumnRef.Base("metadata"), new TableRef.Base("users")).orElseThrow();

        assertInstanceOf(Column.Known.class, idCol);
        assertEquals(SQLType.INTEGER, ((Column.Known) idCol).type());
        assertFalse(idCol.nullable());

        assertInstanceOf(Column.Known.class, nameCol);
        assertTrue(nameCol.nullable());

        assertInstanceOf(Column.Unknown.class, metaCol);
    }

    private Table createSampleTable() {
        return new Table(
                new Table.Catalog.Unknown(),
                new Table.Schema.Unknown(),
                "users",
                new Column[]{
                        new Column.Known("id", SQLType.INTEGER, false),
                        new Column.Known("name", SQLType.VARCHAR, true),
                        new Column.Known("email", SQLType.VARCHAR, true)
                }
        );
    }
}