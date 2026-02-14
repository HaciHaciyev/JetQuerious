package io.github.hacihaciyev.schema.internal;

import io.github.hacihaciyev.sql.TableRef;
import io.github.hacihaciyev.schema.SchemaVerificationException;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SchemaResolverTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private ResultSet tablesResultSet;

    @Mock
    private ResultSet columnsResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        var _ = MockitoAnnotations.openMocks(this);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
    }

    @Test
    void load_simpleTable_success() throws SQLException {
        var tableRef = new TableRef.Base("users");
        setupSuccessfulTableLookup("users", null, null);

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Ok.class, result);
        var table = ((Ok<Table, SchemaVerificationException>) result).value();
        assertEquals("users", table.name());
        assertEquals(2, table.columns().length);
    }

    @Test
    void load_tableWithSchema_success() throws SQLException {
        var tableRef = new TableRef.WithSchema("public", "users");
        setupSuccessfulTableLookup("users", "public", null);

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Ok.class, result);
        var table = ((Ok<Table, SchemaVerificationException>) result).value();
        assertEquals("users", table.name());
        assertInstanceOf(Table.Schema.Known.class, table.schema());
    }

    @Test
    void load_tableWithCatalog_success() throws SQLException {
        var tableRef = new TableRef.WithCatalogAndSchema("mydb", "public", "users");
        setupSuccessfulTableLookup("users", "public", "mydb");

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Ok.class, result);
        var table = ((Ok<Table, SchemaVerificationException>) result).value();
        assertEquals("users", table.name());
        assertInstanceOf(Table.Catalog.Known.class, table.catalog());
        assertInstanceOf(Table.Schema.Known.class, table.schema());
    }

    @Test
    void load_tableNotFound_returnsError() throws SQLException {
        var tableRef = new TableRef.Base("nonexistent");
        when(metaData.getTables(isNull(), isNull(), eq("nonexistent"), any()))
                .thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(false);

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Err.class, result);
        var error = ((Err<Table, SchemaVerificationException>) result).err();
        assertTrue(error.getMessage().contains("Table not found"));
    }

    @Test
    void load_tableWithNoColumns_returnsError() throws SQLException {
        var tableRef = new TableRef.Base("empty_table");
        when(metaData.getTables(isNull(), isNull(), eq("empty_table"), any()))
                .thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(true);
        when(tablesResultSet.getString("TABLE_CAT")).thenReturn(null);
        when(tablesResultSet.getString("TABLE_SCHEM")).thenReturn(null);

        when(metaData.getColumns(isNull(), isNull(), eq("empty_table"), isNull()))
                .thenReturn(columnsResultSet);
        when(columnsResultSet.next()).thenReturn(false);

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Err.class, result);
        var error = ((Err<Table, SchemaVerificationException>) result).err();
        assertTrue(error.getMessage().contains("no accessible columns"));
    }

    @Test
    void load_sqlException_returnsError() throws SQLException {
        var tableRef = new TableRef.Base("users");
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Err.class, result);
    }

    @Test
    void load_cachedTable_returnsCachedValue() throws SQLException {
        var tableRef = new TableRef.Base("cached_users");
        setupSuccessfulTableLookup("cached_users", null, null);

        var result1 = SchemaResolver.load(tableRef, dataSource);

        var result2 = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Ok.class, result1);
        assertInstanceOf(Ok.class, result2);
        verify(dataSource, times(1)).getConnection();
    }

    @Test
    void load_nullTableRef_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> SchemaResolver.load(null, dataSource));
    }

    @Test
    void load_nullDataSource_throwsNullPointerException() {
        var tableRef = new TableRef.Base("users");
        assertThrows(NullPointerException.class,
                () -> SchemaResolver.load(tableRef, null));
    }

    @Test
    void load_unknownColumnType_createsUnknownColumn() throws SQLException {
        var tableRef = new TableRef.Base("special_table");
        when(metaData.getTables(isNull(), isNull(), eq("special_table"), any()))
                .thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(true);
        when(tablesResultSet.getString("TABLE_CAT")).thenReturn(null);
        when(tablesResultSet.getString("TABLE_SCHEM")).thenReturn(null);

        when(metaData.getColumns(isNull(), isNull(), eq("special_table"), isNull()))
                .thenReturn(columnsResultSet);
        when(columnsResultSet.next()).thenReturn(true, false);
        when(columnsResultSet.getString("COLUMN_NAME")).thenReturn("weird_col");
        when(columnsResultSet.getString("TYPE_NAME")).thenReturn("UNKNOWN_TYPE_XYZ");
        when(columnsResultSet.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNullable);

        var result = SchemaResolver.load(tableRef, dataSource);

        assertInstanceOf(Ok.class, result);
        var table = ((Ok<Table, SchemaVerificationException>) result).value();
        assertEquals(1, table.columns().length);
        assertInstanceOf(Column.Unknown.class, table.columns()[0]);
    }

    private void setupSuccessfulTableLookup(String tableName, String schema, String catalog) throws SQLException {
        when(metaData.getTables(eq(catalog), eq(schema), eq(tableName), any()))
                .thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(true);
        when(tablesResultSet.getString("TABLE_CAT")).thenReturn(catalog);
        when(tablesResultSet.getString("TABLE_SCHEM")).thenReturn(schema);

        setupColumnsResultSet(catalog, schema, tableName);
    }

    private void setupColumnsResultSet(String catalog, String schema, String tableName) throws SQLException {
        when(metaData.getColumns(eq(catalog), eq(schema), eq(tableName), isNull()))
                .thenReturn(columnsResultSet);

        when(columnsResultSet.next()).thenReturn(true, true, false);

        when(columnsResultSet.getString("COLUMN_NAME")).thenReturn("id", "name");
        when(columnsResultSet.getString("TYPE_NAME")).thenReturn("INTEGER", "VARCHAR");
        when(columnsResultSet.getInt("NULLABLE")).thenReturn(
                DatabaseMetaData.columnNoNulls,
                DatabaseMetaData.columnNullable
        );
    }
}