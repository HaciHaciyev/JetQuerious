package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.*;
import io.github.hacihaciyev.util.DBTestContainer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

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
    
    private static Context.Select selectCtx(String table, String... cols) {
        var refs = Arrays.stream(cols).map(c -> (Ref) namedRef(c)).toList();
                
        return ContextFactory.selectContext(
                List.of(physical(table)),
                refs,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.empty()
        );
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
    void select_whereAmbiguousColumn_fails() {
        var where = new BinaryOp(BinaryOp.BinaryOperator.EQ,
                new ColumnRef.Base("id"), new Literal.IntLiteral(1));

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
                List.of(physical("users"), physical("orders")),
                List.of(aliasedRef("users", "id", "user_id")),
                Optional.of(where),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.empty()
        ));
    }

    @Test
    @Order(15)
    void select_joinTable_columnFromJoinedTable_passes() {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
                List.of(physical("users", "u"), physical("orders", "o")),
                List.of(aliasedRef("u", "name", "user_name"), aliasedRef("o", "status", "order_status")),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.empty()
        ));
    }

    @Test
    @Order(16)
    void select_joinTable_nonExistentColumnInOn_fails() {
        var where = new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("ghost"), new Literal.IntLiteral(1));

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
                List.of(physical("users", "u"), physical("orders", "o")),
                List.of(aliasedRef("u", "id", "uid")),
                Optional.of(where),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.empty()
        ));
    }

    @Test
    @Order(17)
    void select_having_nonExistentColumn_fails() {
        var having = new BinaryOp(BinaryOp.BinaryOperator.GT, new ColumnRef.Base("ghost"), new Literal.IntLiteral(0));

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
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
    @Order(18)
    void select_groupBy_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
                List.of(physical("orders")),
                List.of(namedRef("status")),
                Optional.empty(),
                List.of(new ColumnRef.Base("ghost")),
                Optional.empty(),
                List.of(),
                Optional.empty()
        ));
    }

    @Test
    @Order(19)
    void select_orderBy_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
                List.of(physical("users")),
                List.of(namedRef("name")),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(new ColumnRef.Base("ghost")),
                Optional.empty()
        ));
    }

    @Test
    @Order(20)
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
    @Order(21)
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
    @Order(22)
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
    @Order(23)
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
    @Order(24)
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
    @Order(25)
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
    @Order(26)
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
    @Order(27)
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
    @Order(28)
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
    @Order(29)
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
    @Order(30)
    void delete_noWhere_passes() {
        assertDoesNotThrow(() -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Test
    @Order(31)
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
    @Order(32)
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
    @Order(33)
    void delete_returning_validColumn_passes() {
        assertDoesNotThrow(() -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(namedRef("id")),
            Optional.empty()
        ));
    }

    @Test
    @Order(34)
    void delete_returning_nonExistentColumn_fails() {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(namedRef("ghost")),
            Optional.empty()
        ));
    }
    
    @Test
    @Order(35)
    void union_sameColumnCount_passes() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertDoesNotThrow(() -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(36)
    void union_differentColumnCounts_fails() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(37)
    void union_threeQueriesDifferentCounts_fails() {
        var q1 = selectCtx("users",       "id", "name");
        var q2 = selectCtx("orders",      "id", "status");
        var q3 = selectCtx("order_items", "id");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2, q3),
                UnionType.UNION,
                List.of(),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(38)
    void union_orderByColumnInFirstProjection_passes() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertDoesNotThrow(() -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(new ColumnRef.Base("id")),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(39)
    void union_orderByColumnNotInFirstProjection_fails() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(new ColumnRef.Base("status")),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(40)
    void union_orderByGhostColumn_fails() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(new ColumnRef.Base("ghost")),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(41)
    void union_orderByMultipleColumns_allInFirstProjection_passes() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertDoesNotThrow(() -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(new ColumnRef.Base("id"), new ColumnRef.Base("name")),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(42)
    void union_orderByMultipleColumns_oneGhost_fails() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION,
                List.of(new ColumnRef.Base("id"), new ColumnRef.Base("ghost")),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(43)
    void unionAll_sameColumnCount_passes() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id", "status");
    
        assertDoesNotThrow(() -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.UNION_ALL,
                List.of(),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(44)
    void intersect_differentColumnCounts_fails() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.INTERSECT,
                List.of(),
                Optional.empty()
        ));
    }
    
    @Test
    @Order(45)
    void except_differentColumnCounts_fails() {
        var q1 = selectCtx("users",  "id", "name");
        var q2 = selectCtx("orders", "id");
    
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.unionContext(
                List.of(q1, q2),
                UnionType.EXCEPT,
                List.of(),
                Optional.empty()
        ));
    }
}