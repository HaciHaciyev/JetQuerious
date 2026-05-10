package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContextTest {

    private static TableSource.Physical physical(String table) {
        return new TableSource.Physical(new TableRef.Base(table));
    }

    private static TableSource.Physical physical(String table, String alias) {
        return new TableSource.Physical(new TableRef.AliasedBase(table, alias));
    }

    private static Ref namedRef(String col) {
        return new Ref.Named(new Projection.Base(new ColumnRef.Base(col)));
    }

    private static Ref namedRef(String col, Class<?> type) {
        return new Ref.Named(new Projection.Base(new ColumnRef.Base(col, new ColumnRef.Type.Some(type))));
    }

    private static Ref namedRef(String table, String col) {
        return new Ref.Named(new Projection.Base(new ColumnRef.VariableBase(table, col)));
    }
    
    private static Ref aliasedRef(String table, String col, String alias) {
        return new Ref.Named(new Projection.Base(new ColumnRef.VariableAlias(table, col, alias)));
    }

    @Test
    @Order(1)
    void select_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(2)
    void select_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("nonexistent")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(3)
    void select_ambiguousColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users"), physical("orders")),
            List.of(namedRef("id")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(4)
    void select_qualifiedColumn_resolvesAmbiguity() {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users", "u"), physical("orders", "o")),
            List.of(aliasedRef("u", "id", "user_id"), aliasedRef("o", "id", "order_id")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(5)
    void select_whereWithValidColumn_passes() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("name"),
            new Literal.StringLiteral("Alice")
        );

        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("id")),
            Optional.of(where),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(6)
    void select_whereWithNonExistentColumn_fails() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost"),
            new Literal.IntLiteral(1)
        );

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("id")),
            Optional.of(where),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(7)
    void select_groupBy_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("orders")),
            List.of(namedRef("status")),
            Optional.empty(),
            List.of(new ColumnRef.Base("status")),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(8)
    void select_having_validColumn_passes() {
        var having = new BinaryOp(
            BinaryOp.BinaryOperator.GT,
            new Func.Count(new ColumnRef.Base("id"), false),
            new Literal.IntLiteral(1)
        );

        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("orders")),
            List.of(namedRef("status")),
            Optional.empty(),
            List.of(new ColumnRef.Base("status")),
            Optional.of(having),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(9)
    void select_orderBy_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(new ColumnRef.Base("email")),
            Optional.empty()
        ));
    }

    @Test
    @Order(10)
    void select_typeMismatch_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("id", String.class)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(11)
    void select_correctType_passes() {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("id", Long.class)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(12)
    void select_nonExistentTable_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("ghost_table")),
            List.of(namedRef("id")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(13)
    void select_duplicateTableNames_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users"), physical("users")),
            List.of(namedRef("id")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(14)
    void insert_validColumns_passes() {
        assertDoesNotThrow(() -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef("name"), namedRef("email")),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(15)
    void insert_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef("ghost")),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(16)
    void insert_returning_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(namedRef("id")),
            Optional.empty()
        ));
    }

    @Test
    @Order(17)
    void insert_returning_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(namedRef("ghost")),
            Optional.empty()
        ));
    }

    @Test
    @Order(18)
    void update_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(19)
    void update_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("ghost")),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(20)
    void update_whereWithValidColumn_passes() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        assertDoesNotThrow(() -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            List.of(),
            Optional.of(where),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(21)
    void update_whereWithNonExistentColumn_fails() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost"),
            new Literal.LongLiteral(1L)
        );

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            List.of(),
            Optional.of(where),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(22)
    void update_returning_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            List.of(),
            Optional.empty(),
            List.of(namedRef("id")),
            Optional.empty()
        ));
    }

    @Test
    @Order(23)
    void update_returning_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            List.of(),
            Optional.empty(),
            List.of(namedRef("ghost")),
            Optional.empty()
        ));
    }

    @Test
    @Order(24)
    void delete_noWhere_passes() {
        assertDoesNotThrow(() -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(25)
    void delete_whereWithValidColumn_passes() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        assertDoesNotThrow(() -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.of(where),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(26)
    void delete_whereWithNonExistentColumn_fails() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost"),
            new Literal.LongLiteral(1L)
        );

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.of(where),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(27)
    void delete_returning_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(namedRef("id")),
            Optional.empty()
        ));
    }

    @Test
    @Order(28)
    void delete_returning_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(namedRef("ghost")),
            Optional.empty()
        ));
    }
}