package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.builders.*;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class OuterContextTest {

    private static TableSource.Physical physical(String table) {
        return new TableSource.Physical(new TableRef.Base(table));
    }

    private static TableSource.Physical physical(String table, String alias) {
        return new TableSource.Physical(new TableRef.AliasedBase(table, alias));
    }

    private static Ref namedRef(String col) {
        return new Ref.Named(new Projection.Base(new ColumnRef.Base(col)));
    }

    private static Ref aliasedRef(String table, String col, String alias) {
        return new Ref.Named(new Projection.Base(new ColumnRef.VariableAlias(table, col, alias)));
    }

    private static JQ.Read selectJq(String sql, Context.Select ctx) {
        return new JQ.Read(sql, ctx);
    }

    private static Context.Select selectCtx(String table, String... cols) {
        var refs = java.util.Arrays.stream(cols)
            .map(c -> (Ref) namedRef(c))
            .toList();
        
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

    private static Context.Select selectCtxWithOuter(String table, String col, JQ.Read outer) {
        return ContextFactory.selectContext(
            List.of(physical(table)),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.of((Context) outer.context())
        );
    }

    @Test
    void insert_withOuterContext_columnFromOuter_passes() {
        var outerCtx = selectCtx("orders", "id", "status");
        var outerJq  = selectJq("SELECT id, status FROM orders", outerCtx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("user_id"),
            new ColumnRef.Base("id")
        );

        assertDoesNotThrow(() ->
            new InsertBuilder("users")
                .columns("name", String.class)
                .build(outerJq)
        );
    }

    @Test
    void insert_withOuterContext_nonExistentColumnInOuter_fails() {
        var outerCtx = selectCtx("orders", "id");
        var outerJq  = selectJq("SELECT id FROM orders", outerCtx);

        assertThrows(SchemaVerificationException.class, () ->
            ContextFactory.insertContext(
                List.of(physical("users")),
                List.of(namedRef("ghost_column")),
                Optional.empty(),
                List.of(),
                Optional.of((Context) outerJq.context())
            )
        );
    }

    @Test
    void insert_twoLevelsOuter_columnFromTwoLevelsUp_passes() {
        var level2Ctx = selectCtx("orders", "id", "status");
        var level2Jq  = selectJq("SELECT id, status FROM orders", level2Ctx);

        var level1Ctx = selectCtxWithOuter("order_items", "id", level2Jq);
        var level1Jq  = selectJq("SELECT id FROM order_items", level1Ctx);

        assertDoesNotThrow(() ->
            new InsertBuilder("users")
                .columns("name", String.class)
                .build(level1Jq)
        );
    }

    @Test
    void update_withOuterContext_whereReferencesOuterColumn_passes() {
        var outerCtx = selectCtx("orders", "id", "user_id");
        var outerJq  = selectJq("SELECT id, user_id FROM orders", outerCtx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new ColumnRef.Base("user_id")
        );

        assertDoesNotThrow(() ->
            new UpdateBuilder("users")
                .set("name", String.class)
                .where(where)
                .build(outerJq)
        );
    }

    @Test
    void update_withOuterContext_computedExprReferencesOuterColumn_passes() {
        var outerCtx = selectCtx("orders", "total");
        var outerJq  = selectJq("SELECT total FROM orders", outerCtx);

        var expr = new BinaryOp(
            BinaryOp.BinaryOperator.PLUS,
            new ColumnRef.Base("qty"),
            new ColumnRef.Base("total")
        );

        assertDoesNotThrow(() ->
            new UpdateBuilder("order_items")
                .set("qty", expr)
                .build(outerJq)
        );
    }

    @Test
    void update_withOuterContext_ghostColumnNotInAnyLevel_fails() {
        var outerCtx = selectCtx("orders", "id");
        var outerJq  = selectJq("SELECT id FROM orders", outerCtx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost_column"),
            new Literal.LongLiteral(1L)
        );

        assertThrows(SchemaVerificationException.class, () ->
            new UpdateBuilder("users")
                .set("name", String.class)
                .where(where)
                .build(outerJq)
        );
    }

    @Test
    void update_twoLevelsOuter_columnFromTwoLevelsUp_passes() {
        var level2Ctx = selectCtx("users", "id", "name");
        var level2Jq  = selectJq("SELECT id, name FROM users", level2Ctx);

        var level1Ctx = selectCtxWithOuter("orders", "id", level2Jq);
        var level1Jq  = selectJq("SELECT id FROM orders", level1Ctx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("order_id"),
            new ColumnRef.Base("id")
        );

        assertDoesNotThrow(() ->
            new UpdateBuilder("order_items")
                .set("product", String.class)
                .where(where)
                .build(level1Jq)
        );
    }

    @Test
    void delete_withOuterContext_whereReferencesOuterColumn_passes() {
        var outerCtx = selectCtx("orders", "id", "user_id");
        var outerJq  = selectJq("SELECT id, user_id FROM orders", outerCtx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new ColumnRef.Base("user_id")
        );

        assertDoesNotThrow(() ->
            new DeleteBuilder("users")
                .where(where)
                .build(outerJq)
        );
    }

    @Test
    void delete_withOuterContext_ghostColumnNotInAnyLevel_fails() {
        var outerCtx = selectCtx("orders", "id");
        var outerJq  = selectJq("SELECT id FROM orders", outerCtx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost_column"),
            new Literal.LongLiteral(1L)
        );

        assertThrows(SchemaVerificationException.class, () ->
            new DeleteBuilder("users")
                .where(where)
                .build(outerJq)
        );
    }

    @Test
    void delete_twoLevelsOuter_columnFromTwoLevelsUp_passes() {
        var level2Ctx = selectCtx("users", "id");
        var level2Jq  = selectJq("SELECT id FROM users", level2Ctx);

        var level1Ctx = selectCtxWithOuter("orders", "id", level2Jq);
        var level1Jq  = selectJq("SELECT id FROM orders", level1Ctx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("order_id"),
            new ColumnRef.Base("id")
        );

        assertDoesNotThrow(() ->
            new DeleteBuilder("order_items")
                .where(where)
                .build(level1Jq)
        );
    }

    @Test
    void delete_twoLevelsOuter_ghostColumnNotInAnyLevel_fails() {
        var level2Ctx = selectCtx("users", "id");
        var level2Jq  = selectJq("SELECT id FROM users", level2Ctx);

        var level1Ctx = selectCtxWithOuter("orders", "id", level2Jq);
        var level1Jq  = selectJq("SELECT id FROM orders", level1Ctx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost_column"),
            new Literal.LongLiteral(1L)
        );

        assertThrows(SchemaVerificationException.class, () ->
            new DeleteBuilder("order_items")
                .where(where)
                .build(level1Jq)
        );
    }

    @Test
    void delete_threeLevelsOuter_columnFromThreeLevelsUp_passes() {
        var level3Ctx = selectCtx("users", "id");
        var level3Jq  = selectJq("SELECT id FROM users", level3Ctx);

        var level2Ctx = selectCtxWithOuter("orders", "id", level3Jq);
        var level2Jq  = selectJq("SELECT id FROM orders", level2Ctx);

        var level1Ctx = selectCtxWithOuter("order_items", "id", level2Jq);
        var level1Jq  = selectJq("SELECT id FROM order_items", level1Ctx);

        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        assertDoesNotThrow(() ->
            new DeleteBuilder("users")
                .where(where)
                .build(level1Jq)
        );
    }
}