package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
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

    private static JQ.Read usersIdName() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("name"))
            .from("users")
            .build();
    }

    private static JQ.Read ordersIdStatus() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("status"))
            .from("orders")
            .build();
    }

    private static JQ.Read itemsIdProduct() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("product"))
            .from("order_items")
            .build();
    }

    private static JQ.Read usersIdOnly() {
        return SelectBuilder.select(new ColumnRef.Base("id"))
            .from("users")
            .build();
    }

    @Test
    void union() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus()).build();

        assertInstanceOf(JQ.Read.class, jq);
        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void unionAll() {
        var jq = new UnionBuilder(UnionType.UNION_ALL, usersIdName(), ordersIdStatus()).build();

        assertEquals(
            "SELECT id, name FROM users UNION ALL SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void intersect() {
        var jq = new UnionBuilder(UnionType.INTERSECT, usersIdName(), ordersIdStatus()).build();

        assertEquals(
            "SELECT id, name FROM users INTERSECT SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void except() {
        var jq = new UnionBuilder(UnionType.EXCEPT, usersIdName(), ordersIdStatus()).build();

        assertEquals(
            "SELECT id, name FROM users EXCEPT SELECT id, status FROM orders",
            jq.sql()
        );
    }

    @Test
    void add_thirdQuery() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .add(itemsIdProduct())
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
    void add_isImmutable() {
        var builder = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus());
        var extended = builder.add(itemsIdProduct());

        assertEquals(2, builder.build().sql().split("UNION").length);
        assertEquals(3, extended.build().sql().split("UNION").length);
    }

    @Test
    void withOrderBy_strings() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .orderBy("id")
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id",
            jq.sql()
        );
    }

    @Test
    void withOrderBy_expr() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .orderBy(new ColumnRef.Base("id"))
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id",
            jq.sql()
        );
    }

    @Test
    void withOrderBy_multipleColumns() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .orderBy("id", "name")
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id, name",
            jq.sql()
        );
    }

    @Test
    void withOrderBy_nonExistentColumn_throws() {
        assertThrows(SchemaVerificationException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
                .orderBy("ghost_column")
                .build()
        );
    }

    @Test
    void withOrderBy_columnFromSecondQueryOnly_throws() {
        assertThrows(SchemaVerificationException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
                .orderBy("status")
                .build()
        );
    }

    @Test
    void withLimit() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .limit(10)
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders LIMIT 10",
            jq.sql()
        );
    }

    @Test
    void withLimitAndOffset() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .limit(10)
            .offset(20);

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders LIMIT 10 OFFSET 20",
            jq.sql()
        );
    }

    @Test
    void withOrderByAndLimit() {
        var jq = new UnionBuilder(UnionType.UNION_ALL, usersIdName(), ordersIdStatus())
            .orderBy("id")
            .limit(5)
            .build();

        assertEquals(
            "SELECT id, name FROM users UNION ALL SELECT id, status FROM orders ORDER BY id LIMIT 5",
            jq.sql()
        );
    }

    @Test
    void withOrderByLimitAndOffset() {
        var jq = new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
            .orderBy("id")
            .limit(10)
            .offset(20);

        assertEquals(
            "SELECT id, name FROM users UNION SELECT id, status FROM orders ORDER BY id LIMIT 10 OFFSET 20",
            jq.sql()
        );
    }

    @Test
    void differentColumnCounts_throws() {
        assertThrows(SchemaVerificationException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), usersIdOnly()).build()
        );
    }

    @Test
    void differentColumnCounts_inAdd_throws() {
        assertThrows(SchemaVerificationException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
                .add(usersIdOnly())
                .build()
        );
    }

    @Test
    void threeQueriesDifferentCounts_throws() {
        assertThrows(SchemaVerificationException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
                .add(usersIdOnly())
                .build()
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

        var jq = new UnionBuilder(UnionType.UNION, filtered, ordersIdStatus()).build();

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
            new UnionBuilder(null, usersIdName(), ordersIdStatus())
        );
    }

    @Test
    void nullFirstQuery_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, null, ordersIdStatus())
        );
    }

    @Test
    void nullInRest_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), (JQ.Read) null)
        );
    }

    @Test
    void nullAddQuery_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus()).add(null)
        );
    }

    @Test
    void emptyOrderBy_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
                .orderBy(new ColumnRef.Base[0])
        );
    }

    @Test
    void nullOrderByExpr_throws() {
        assertThrows(NullPointerException.class, () ->
            new UnionBuilder(UnionType.UNION, usersIdName(), ordersIdStatus())
                .orderBy((ColumnRef.Base) null)
        );
    }
}