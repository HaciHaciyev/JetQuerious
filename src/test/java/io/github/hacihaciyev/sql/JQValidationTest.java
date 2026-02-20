package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.sql.internal.schema.Column;
import io.github.hacihaciyev.sql.internal.schema.SchemaResolver;
import io.github.hacihaciyev.sql.internal.schema.Table;
import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.util.Ok;
import io.github.hacihaciyev.util.Err;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class JQValidationTest {

    @BeforeEach
    void setup() {
        DataSource mockDataSource = mock(DataSource.class);
        Conf.INSTANCE.defDataSource(mockDataSource);
    }

    @Test
    @DisplayName("Should create Read query with valid table and columns")
    void shouldCreateReadWithValidTableAndColumns() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            assertDoesNotThrow(() -> new JQ.Read(
                "SELECT id, name FROM users",
                new TableRef[]{new TableRef.Base("users")},
                new ColumnRef[]{
                    new ColumnRef.Base("id"),
                    new ColumnRef.Base("name")
                }
            ));
        }
    }

    @Test
    @DisplayName("Should create Write query with type validation")
    void shouldCreateWriteWithTypeValidation() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            assertDoesNotThrow(() -> new JQ.Write(
                "INSERT INTO users (id, name) VALUES (?, ?)",
                new TableRef[]{new TableRef.Base("users")},
                new ColumnRef[]{
                    new ColumnRef.Base("id", new ColumnRef.Type.Some(Integer.class)),
                    new ColumnRef.Base("name", new ColumnRef.Type.Some(String.class))
                }
            ));
        }
    }

    @Test
    @DisplayName("Should accept empty table and column refs")
    void shouldAcceptEmptyRefs() {
        assertDoesNotThrow(() -> new JQ.Read(
            "SELECT 1",
            new TableRef[]{},
            new ColumnRef[]{}
        ));
    }

    @Test
    @DisplayName("Should throw when table not found")
    void shouldThrowWhenTableNotFound() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any()))
                .thenReturn(new Err<>(new SchemaVerificationException("Table not found")));

            var exception = assertThrows(SchemaVerificationException.class, () ->
                new JQ.Read(
                    "SELECT * FROM nonexistent",
                    new TableRef[]{new TableRef.Base("nonexistent")},
                    new ColumnRef[]{}
                )
            );

            assertTrue(exception.getMessage().contains("not found or failed to load"));
        }
    }

    @Test
    @DisplayName("Should throw when column not found")
    void shouldThrowWhenColumnNotFound() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            var exception = assertThrows(SchemaVerificationException.class, () ->
                new JQ.Read(
                    "SELECT invalid_column FROM users",
                    new TableRef[]{new TableRef.Base("users")},
                    new ColumnRef[]{new ColumnRef.Base("invalid_column")}
                )
            );

            assertTrue(exception.getMessage().contains("not found in any of the referenced tables"));
        }
    }

    @Test
    @DisplayName("Should throw when Java type incompatible with SQL type")
    void shouldThrowWhenTypeIncompatible() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            var exception = assertThrows(SchemaVerificationException.class, () ->
                new JQ.Write(
                    "INSERT INTO users (name) VALUES (?)",
                    new TableRef[]{new TableRef.Base("users")},
                    new ColumnRef[]{
                        new ColumnRef.Base("name", new ColumnRef.Type.Some(Integer.class))
                    }
                )
            );

            assertTrue(exception.getMessage().contains("Type mismatch"));
            assertTrue(exception.getMessage().contains("Integer"));
        }
    }

    @Test
    @DisplayName("Should throw for unsupported Java type")
    void shouldThrowForUnsupportedType() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            class CustomType {}

            var exception = assertThrows(SchemaVerificationException.class, () ->
                new JQ.Write(
                    "INSERT INTO users (name) VALUES (?)",
                    new TableRef[]{new TableRef.Base("users")},
                    new ColumnRef[]{
                        new ColumnRef.Base("name", new ColumnRef.Type.Some(CustomType.class))
                    }
                )
            );

            assertTrue(exception.getMessage().contains("Unsupported Java type"));
            assertTrue(exception.getMessage().contains("CustomType"));
        }
    }

    @Test
    @DisplayName("Should validate multiple tables and columns with aliases")
    void shouldValidateMultipleTables() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(argThat(ref -> ref.name().equalsIgnoreCase("users")), any()))
                .thenReturn(new Ok<>(createUsersTable()));
            mock.when(() -> SchemaResolver.load(argThat(ref -> ref.name().equalsIgnoreCase("orders")), any()))
                .thenReturn(new Ok<>(createOrdersTable()));

            assertDoesNotThrow(() -> new JQ.Read(
                "SELECT u.id, o.id FROM users u JOIN orders o ON u.id = o.user_id",
                new TableRef[]{
                    new TableRef.AliasedBase("users", "u"),
                    new TableRef.AliasedBase("orders", "o")
                },
                new ColumnRef[]{
                    new ColumnRef.VariableBase("u", "id"),
                    new ColumnRef.VariableBase("o", "id")
                }
            ));
        }
    }

    @Test
    @DisplayName("Should skip type validation for unknown columns")
    void shouldSkipTypeValidationForUnknownColumns() {
        var table = new Table(
            new Table.Catalog.Unknown(),
            new Table.Schema.Unknown(),
            "dynamic_table",
            new Column[]{
                new Column.Unknown("data", true)
            }
        );

        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(table));

            assertDoesNotThrow(() -> new JQ.Read(
                "SELECT data FROM dynamic_table",
                new TableRef[]{new TableRef.Base("dynamic_table")},
                new ColumnRef[]{
                    new ColumnRef.Base("data", new ColumnRef.Type.Some(String.class))
                }
            ));
        }
    }

    @Test
    @DisplayName("Should collect multiple validation errors")
    void shouldCollectMultipleErrors() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            var exception = assertThrows(SchemaVerificationException.class, () ->
                new JQ.Read(
                    "SELECT invalid1, invalid2 FROM users",
                    new TableRef[]{new TableRef.Base("users")},
                    new ColumnRef[]{
                        new ColumnRef.Base("invalid1"),
                        new ColumnRef.Base("invalid2")
                    }
                )
            );

            var message = exception.getMessage();
            assertTrue(message.contains("invalid1"));
            assertTrue(message.contains("invalid2"));
        }
    }

    @Test
    @DisplayName("Should validate VariableAlias with type checking")
    void shouldValidateVariableAliasWithType() {
        try (MockedStatic<SchemaResolver> mock = mockStatic(SchemaResolver.class)) {
            mock.when(() -> SchemaResolver.load(any(), any())).thenReturn(new Ok<>(createUsersTable()));

            assertDoesNotThrow(() -> new JQ.Read(
                "SELECT u.id AS user_id FROM users u",
                new TableRef[]{new TableRef.AliasedBase("users", "u")},
                new ColumnRef[]{
                    new ColumnRef.VariableAlias("u", "id", "user_id", new ColumnRef.Type.Some(Integer.class))
                }
            ));
        }
    }

    private Table createUsersTable() {
        return new Table(
            new Table.Catalog.Unknown(),
            new Table.Schema.Unknown(),
            "users",
            new Column[]{
                new Column.Known("id", SQLType.INTEGER, false),
                new Column.Known("name", SQLType.VARCHAR, false),
                new Column.Known("email", SQLType.VARCHAR, true)
            }
        );
    }

    private Table createOrdersTable() {
        return new Table(
            new Table.Catalog.Unknown(),
            new Table.Schema.Unknown(),
            "orders",
            new Column[]{
                new Column.Known("id", SQLType.INTEGER, false),
                new Column.Known("user_id", SQLType.INTEGER, false),
                new Column.Known("total", SQLType.DECIMAL, false)
            }
        );
    }
}