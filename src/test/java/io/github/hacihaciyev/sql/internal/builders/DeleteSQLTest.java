package io.github.hacihaciyev.sql.internal.builders;

import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeleteSQLTest {

    @Test
    void simple() {
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.empty(),
            List.of()
        );
        assertEquals("DELETE FROM users", sql);
    }

    @Test
    void withSchema() {
        var sql = DeleteSQL.build(
            new TableRef.WithSchema("public", "users"),
            Optional.empty(),
            List.of()
        );
        assertEquals("DELETE FROM public.users", sql);
    }

    @Test
    void withAlias() {
        var sql = DeleteSQL.build(
            new TableRef.AliasedBase("users", "u"),
            Optional.empty(),
            List.of()
        );
        assertEquals("DELETE FROM users AS u", sql);
    }

    @Test
    void withWhere() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.of(where),
            List.of()
        );
        assertEquals("DELETE FROM users WHERE (id = 1)", sql);
    }

    @Test
    void withWhere_complex() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.AND,
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("id"), new Literal.LongLiteral(1L)),
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("active"), new Literal.BooleanLiteral(true))
        );
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.of(where),
            List.of()
        );
        assertEquals("DELETE FROM users WHERE ((id = 1) AND (active = TRUE))", sql);
    }

    @Test
    void withWhere_isNull() {
        var where = new IsNullExpr.IsNull(new ColumnRef.Base("email"));
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.of(where),
            List.of()
        );
        assertEquals("DELETE FROM users WHERE email IS NULL", sql);
    }

    @Test
    void withWhere_in() {
        var where = new InExpr.In(
            new ColumnRef.Base("id"),
            new InExpr.ValueList(List.of(
                new Literal.LongLiteral(1L),
                new Literal.LongLiteral(2L),
                new Literal.LongLiteral(3L)
            ))
        );
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.of(where),
            List.of()
        );
        assertEquals("DELETE FROM users WHERE id IN (1, 2, 3)", sql);
    }

    @Test
    void withReturning() {
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.empty(),
            List.of("id", "name")
        );
        assertEquals("DELETE FROM users RETURNING id, name", sql);
    }

    @Test
    void withReturning_single() {
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.empty(),
            List.of("id")
        );
        assertEquals("DELETE FROM users RETURNING id", sql);
    }

    @Test
    void withWhereAndReturning() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );
        var sql = DeleteSQL.build(
            new TableRef.Base("users"),
            Optional.of(where),
            List.of("id", "name", "email")
        );
        assertEquals("DELETE FROM users WHERE (id = 1) RETURNING id, name, email", sql);
    }
}