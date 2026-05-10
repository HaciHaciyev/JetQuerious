package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class UpdateBuilderTest {

    @Test
    void singleParam() {
        var jq = new UpdateBuilder("users")
            .set("name", String.class)
            .build();

        assertInstanceOf(JQ.Write.class, jq);
        assertEquals("UPDATE users SET name = ?", jq.sql());
    }

    @Test
    void multipleParams() {
        var jq = new UpdateBuilder("users")
            .set("name", String.class, "email", String.class)
            .build();

        assertEquals("UPDATE users SET name = ?, email = ?", jq.sql());
    }

    @Test
    void withTableRef() {
        var jq = new UpdateBuilder(new TableRef.Base("users"))
            .set("name", String.class)
            .build();

        assertEquals("UPDATE users SET name = ?", jq.sql());
    }

    @Test
    void computed_expression() {
        var expr = new BinaryOp(
            BinaryOp.BinaryOperator.PLUS,
            new ColumnRef.Base("qty"),
            new Literal.IntLiteral(1)
        );

        var jq = new UpdateBuilder("order_items")
            .set("qty", expr)
            .build();

        assertEquals("UPDATE order_items SET qty = (qty + 1)", jq.sql());
    }

    @Test
    void mixed_paramAndComputed() {
        var expr = new BinaryOp(
            BinaryOp.BinaryOperator.PLUS,
            new ColumnRef.Base("qty"),
            new Literal.IntLiteral(1)
        );

        var jq = new UpdateBuilder("order_items")
            .set("product", String.class, "qty", expr)
            .build();

        assertEquals("UPDATE order_items SET product = ?, qty = (qty + 1)", jq.sql());
    }

    @Test
    void withWhere() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        var jq = new UpdateBuilder("users")
            .set("name", String.class)
            .where(where)
            .build();

        assertEquals("UPDATE users SET name = ? WHERE (id = 1)", jq.sql());
    }

    @Test
    void withWhere_complex() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.AND,
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("id"), new Literal.LongLiteral(1L)),
            new BinaryOp(BinaryOp.BinaryOperator.EQ, new ColumnRef.Base("active"), new Literal.BooleanLiteral(true))
        );

        var jq = new UpdateBuilder("users")
            .set("name", String.class)
            .where(where)
            .build();

        assertEquals("UPDATE users SET name = ? WHERE ((id = 1) AND (active = TRUE))", jq.sql());
    }

    @Test
    void withReturning_strings() {
        var jq = new UpdateBuilder("users")
            .set("name", String.class)
            .returning("id", "name")
            .build();

        assertEquals("UPDATE users SET name = ? RETURNING id, name", jq.sql());
    }

    @Test
    void withWhereAndReturning() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("id"),
            new Literal.LongLiteral(1L)
        );

        var jq = new UpdateBuilder("users")
            .set("name", String.class, "email", String.class)
            .where(where)
            .returning("id", "name", "email")
            .build();

        assertEquals("UPDATE users SET name = ?, email = ? WHERE (id = 1) RETURNING id, name, email", jq.sql());
    }

    @Test
    void nullTableRef_throws() {
        assertThrows(NullPointerException.class, () -> new UpdateBuilder((TableRef) null));
    }

    @Test
    void emptySet_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new UpdateBuilder("users").set());
    }

    @Test
    void oddSetPairs_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new UpdateBuilder("users").set("name"));
    }

    @Test
    void blankColumnName_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new UpdateBuilder("users").set("  ", String.class));
    }

    @Test
    void nullWhereCondition_throws() {
        assertThrows(NullPointerException.class, () ->
            new UpdateBuilder("users").set("name", String.class).where(null));
    }

    @Test
    void nonExistentTable_throws() {
        assertThrows(Exception.class, () ->
            new UpdateBuilder("ghost_table")
                .set("name", String.class)
                .build());
    }

    @Test
    void nonExistentColumn_throws() {
        assertThrows(Exception.class, () ->
            new UpdateBuilder("users")
                .set("ghost_column", String.class)
                .build());
    }
    
    @Test
    void nonExistentColumnInComputedExpr_throws() {
        var expr = new BinaryOp(
            BinaryOp.BinaryOperator.PLUS,
            new ColumnRef.Base("ghost_column"),
            new Literal.IntLiteral(1)
        );
    
        assertThrows(Exception.class, () ->
            new UpdateBuilder("users")
                .set("name", expr)
                .build());
    }
    
    @Test
    void nonExistentColumnInWhere_throws() {
        var where = new BinaryOp(
            BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base("ghost_column"),
            new Literal.LongLiteral(1L)
        );
    
        assertThrows(Exception.class, () ->
            new UpdateBuilder("users")
                .set("name", String.class)
                .where(where)
                .build());
    }
    
    @Test
    void nonExistentColumnInReturning_throws() {
        assertThrows(Exception.class, () ->
            new UpdateBuilder("users")
                .set("name", String.class)
                .returning("ghost_column")
                .build());
    }
}