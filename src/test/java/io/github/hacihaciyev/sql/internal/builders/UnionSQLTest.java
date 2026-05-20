package io.github.hacihaciyev.sql.internal.builders;

import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Literal;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UnionSQLTest {

    @Test
    void union() {
        var sql = UnionSQL.build(
            UnionType.UNION,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals("SELECT id FROM users UNION SELECT id FROM orders", sql);
    }

    @Test
    void unionAll() {
        var sql = UnionSQL.build(
            UnionType.UNION_ALL,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals("SELECT id FROM users UNION ALL SELECT id FROM orders", sql);
    }

    @Test
    void intersect() {
        var sql = UnionSQL.build(
            UnionType.INTERSECT,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals("SELECT id FROM users INTERSECT SELECT id FROM orders", sql);
    }

    @Test
    void except() {
        var sql = UnionSQL.build(
            UnionType.EXCEPT,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals("SELECT id FROM users EXCEPT SELECT id FROM orders", sql);
    }

    @Test
    void threeQueries() {
        var sql = UnionSQL.build(
            UnionType.UNION,
            List.of("SELECT id FROM users", "SELECT id FROM orders", "SELECT id FROM order_items"),
            List.of(),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals(
            "SELECT id FROM users UNION SELECT id FROM orders UNION SELECT id FROM order_items",
            sql
        );
    }

    @Test
    void withOrderBy() {
        var sql = UnionSQL.build(
            UnionType.UNION,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(new ColumnRef.Base("id")),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals("SELECT id FROM users UNION SELECT id FROM orders ORDER BY id", sql);
    }

    @Test
    void withOrderByMultiple() {
        var sql = UnionSQL.build(
            UnionType.UNION_ALL,
            List.of("SELECT id, name FROM users", "SELECT id, name FROM orders"),
            List.of(new ColumnRef.Base("id"), new ColumnRef.Base("name")),
            Optional.empty(),
            Optional.empty()
        );
        assertEquals(
            "SELECT id, name FROM users UNION ALL SELECT id, name FROM orders ORDER BY id, name",
            sql
        );
    }

    @Test
    void withLimit() {
        var sql = UnionSQL.build(
            UnionType.UNION,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(),
            Optional.of(new Limit(10)),
            Optional.empty()
        );
        assertEquals("SELECT id FROM users UNION SELECT id FROM orders LIMIT 10", sql);
    }

    @Test
    void withLimitAndOffset() {
        var sql = UnionSQL.build(
            UnionType.UNION,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(),
            Optional.of(new Limit(10)),
            Optional.of(new Offset(20))
        );
        assertEquals("SELECT id FROM users UNION SELECT id FROM orders LIMIT 10 OFFSET 20", sql);
    }

    @Test
    void withOrderByAndLimit() {
        var sql = UnionSQL.build(
            UnionType.UNION_ALL,
            List.of("SELECT id FROM users", "SELECT id FROM orders"),
            List.of(new ColumnRef.Base("id")),
            Optional.of(new Limit(5)),
            Optional.of(new Offset(10))
        );
        assertEquals(
            "SELECT id FROM users UNION ALL SELECT id FROM orders ORDER BY id LIMIT 5 OFFSET 10",
            sql
        );
    }
}