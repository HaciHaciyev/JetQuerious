package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.BinaryOp;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Literal;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class UnionBuilderTest {

    private static JQ.Read users() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("name"))
            .from("users")
            .build();
    }

    private static JQ.Read orders() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("status"))
            .from("orders")
            .build();
    }

    @Test
    void union() {
        var jq = new UnionBuilder(UnionType.UNION, users(), orders()).build();

        assertInstanceOf(JQ.Read.class, jq);
        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void unionAll() {
        var jq = new UnionBuilder(UnionType.UNION_ALL, users(), orders()).build();

        assertEquals(
            "SELECT id, name FROM users UNION ALL SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void intersect() {
        var jq = new UnionBuilder(UnionType.INTERSECT, users(), orders()).build();

        assertEquals(
            "SELECT id, name FROM users INTERSECT SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void except() {
        var jq = new UnionBuilder(UnionType.EXCEPT, users(), orders()).build();

        assertEquals(
            "SELECT id, name FROM users EXCEPT SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void add_thirdQuery() {
        var items = SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("product"))
            .from("order_items")
            .build();

        var jq = new UnionBuilder(UnionType.UNION, users(), orders())
            .add(items)
            .build();

        assertEquals(
            """
            SELECT id, name FROM users \
            UNION SELECT id, status FROM orders \
            UNION SELECT id, product FROM order_items""",
            jq.sql()
        );
    }

    @Test
    void withOrderBy() {
        var jq = new UnionBuilder(UnionType.UNION, users(), orders())
            .orderBy("id")
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id",
            jq.sql()
        );
    }

    @Test
    void withOrderByMultipleColumns() {
        var jq = new UnionBuilder(UnionType.UNION, users(), orders())
            .orderBy("id", "name")
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id, name",
            jq.sql()
        );
    }

    @Test
    void withOrderByExpr() {
        var jq = new UnionBuilder(UnionType.UNION, users(), orders())
            .orderBy(new ColumnRef.Base("id"))
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id",
            jq.sql()
        );
    }

    @Test
    void withLimit() {
        var jq = new UnionBuilder(UnionType.UNION, users(), orders())
            .limit(10)
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders LIMIT 10",
            jq.sql()
        );
    }

    @Test
    void withLimitAndOffset() {
        var jq = new UnionBuilder(UnionType.UNION, users(), orders())
            .limit(10)
            .offset(20);

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders LIMIT 10 OFFSET 20",
            jq.sql()
        );
    }

    @Test
    void withOrderByAndLimit() {
        var jq = new UnionBuilder(UnionType.UNION_ALL, users(), orders())
            .orderBy("id")
            .limit(5)
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION ALL SELECT id, status FROM orders ORDER BY id LIMIT 5",
            jq.sql()
        );
    }

    @Test
    void withWhereInSubquery() {
        var filtered = SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("name"))
            .from("users")
            .where(new BinaryOp(BinaryOp.BinaryOperator.EQ,
                new ColumnRef.Base("active"),
                new Literal.BooleanLiteral(true)))
            .build();

        var jq = new UnionBuilder(UnionType.UNION, filtered, orders()).build();

        assertEquals(
            """
            SELECT id, name FROM users WHERE (active = TRUE) \
            UNION SELECT id, status FROM orders""",
            jq.sql()
        );
    }

    @Test
    void nullUnionType_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(null, users(), orders()));
    }

    @Test
    void nullFirstQuery_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, null, orders()));
    }

    @Test
    void nullInRest_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, users(), (JQ.Read) null));
    }

    @Test
    void nullAddQuery_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, users(), orders()).add(null));
    }

    @Test
    void emptyOrderBy_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new UnionBuilder(UnionType.UNION, users(), orders()).orderBy(new ColumnRef.Base[0]));
    }
}