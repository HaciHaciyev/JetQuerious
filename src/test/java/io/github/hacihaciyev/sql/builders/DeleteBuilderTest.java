package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class DeleteBuilderTest {

    @Test
    void simple() {
        var jq = new DeleteBuilder("users").build();

        assertInstanceOf(JQ.Write.class, jq);
        assertEquals("DELETE FROM users", jq.sql());
    }

    @Test
    void withTableRef() {
        var jq = new DeleteBuilder(new TableRef.Base("users")).build();

        assertEquals("DELETE FROM users", jq.sql());
    }

    @Test
    void withWhere() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        var jq = new DeleteBuilder("users")
            .where(where)
            .build();

        assertEquals("DELETE FROM users WHERE (id = 1)", jq.sql());
    }

    @Test
    void withWhere_complex() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.AND,
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("id"), new Literal.LongLiteral(1L)),
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("active"), new Literal.BooleanLiteral(true))
        );

        var jq = new DeleteBuilder("users")
            .where(where)
            .build();

        assertEquals("DELETE FROM users WHERE ((id = 1) AND (active = TRUE))", jq.sql());
    }

    @Test
    void withWhere_isNull() {
        var where = new IsNullExpr.IsNull(new ColumnRef.Base("email"));

        var jq = new DeleteBuilder("users")
            .where(where)
            .build();

        assertEquals("DELETE FROM users WHERE email IS NULL", jq.sql());
    }

    @Test
    void withReturning_strings() {
        var jq = new DeleteBuilder("users")
            .returning("id", "name")
            .build();

        assertEquals("DELETE FROM users RETURNING id, name", jq.sql());
    }

    @Test
    void withReturning_afterWhere() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        var jq = new DeleteBuilder("users")
            .where(where)
            .returning("id", "name", "email")
            .build();

        assertEquals("DELETE FROM users WHERE (id = 1) RETURNING id, name, email", jq.sql());
    }

    @Test
    void nullTableRef_throws() {
        assertThrows(NullPointerException.class, () -> new DeleteBuilder((TableRef) null));
    }

    @Test
    void nullWhereCondition_throws() {
        assertThrows(NullPointerException.class, () ->
            new DeleteBuilder("users").where(null));
    }

    @Test
    void nonExistentTable_throws() {
        assertThrows(Exception.class, () ->
            new DeleteBuilder("ghost_table").build());
    }

    @Test
    void nonExistentWhereColumn_throws() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost_column"),
            new Literal.LongLiteral(1L)
        );

        assertThrows(Exception.class, () ->
            new DeleteBuilder("users").where(where).build());
    }

    @Test
    void nonExistentReturningColumn_throws() {
        assertThrows(Exception.class, () ->
            new DeleteBuilder("users").returning("ghost_column").build());
    }
}