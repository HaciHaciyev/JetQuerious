package io.github.hacihaciyev.sql.internal.builders;

import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.internal.value_objects.UpdateEntry;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateSQLTest {

    private static UpdateEntry param(String col) {
        return UpdateEntry.param(new ColumnRef.Base(col), String.class);
    }

    private static UpdateEntry computed(String col, Expr expr) {
        return UpdateEntry.computed(new ColumnRef.Base(col), expr);
    }

    @Test
    void singleParam() {
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(param("name")),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE users SET name = ?", sql);
    }

    @Test
    void multipleParams() {
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(param("name"), param("email")),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE users SET name = ?, email = ?", sql);
    }

    @Test
    void withSchema() {
        var sql = UpdateSQL.build(
            new TableRef.WithSchema("public", "users"),
            List.of(param("name")),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE public.users SET name = ?", sql);
    }

    @Test
    void singleComputed() {
        var expr = new BinaryOp(
            BinaryOp.BinaryOperator.PLUS,
            new ColumnRef.Base("qty"),
            new Literal.IntLiteral(1)
        );
        var sql = UpdateSQL.build(
            new TableRef.Base("order_items"),
            List.of(computed("qty", expr)),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE order_items SET qty = (qty + 1)", sql);
    }

    @Test
    void mixedParamAndComputed() {
        var expr = new BinaryOp(
            BinaryOp.BinaryOperator.PLUS,
            new ColumnRef.Base("qty"),
            new Literal.IntLiteral(1)
        );
        var sql = UpdateSQL.build(
            new TableRef.Base("order_items"),
            List.of(param("product"), computed("qty", expr)),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE order_items SET product = ?, qty = (qty + 1)", sql);
    }

    @Test
    void computed_withNull() {
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(computed("name", new Literal.NullLiteral())),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE users SET name = NULL", sql);
    }

    @Test
    void computed_withLiteral() {
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(computed("active", new Literal.BooleanLiteral(false))),
            Optional.empty(),
            List.of()
        );
        assertEquals("UPDATE users SET active = FALSE", sql);
    }

    @Test
    void withWhere() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(param("name")),
            Optional.of(where),
            List.of()
        );
        assertEquals("UPDATE users SET name = ? WHERE (id = 1)", sql);
    }

    @Test
    void withWhere_complex() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.AND,
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("id"), new Literal.LongLiteral(1L)),
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("active"), new Literal.BooleanLiteral(true))
        );
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(param("name")),
            Optional.of(where),
            List.of()
        );
        assertEquals("UPDATE users SET name = ? WHERE ((id = 1) AND (active = TRUE))", sql);
    }

    @Test
    void withReturning() {
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(param("name")),
            Optional.empty(),
            List.of("id", "name")
        );
        assertEquals("UPDATE users SET name = ? RETURNING id, name", sql);
    }

    @Test
    void withWhereAndReturning() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );
        var sql = UpdateSQL.build(
            new TableRef.Base("users"),
            List.of(param("name"), param("email")),
            Optional.of(where),
            List.of("id", "name", "email")
        );
        assertEquals("UPDATE users SET name = ?, email = ? WHERE (id = 1) RETURNING id, name, email", sql);
    }

    @Test
    void computed_withWhereAndReturning() {
        var expr  = new BinaryOp(BinaryOp.BinaryOperator.PLUS, new ColumnRef.Base("qty"), new Literal.IntLiteral(1));
        var where = new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("id"), new Literal.LongLiteral(42L));
        var sql = UpdateSQL.build(
            new TableRef.Base("order_items"),
            List.of(computed("qty", expr)),
            Optional.of(where),
            List.of("id", "qty")
        );
        assertEquals("UPDATE order_items SET qty = (qty + 1) WHERE (id = 42) RETURNING id, qty", sql);
    }
}