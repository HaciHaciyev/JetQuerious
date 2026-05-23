package io.github.hacihaciyev.sql.internal.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.builders.DeleteBuilder;
import io.github.hacihaciyev.sql.builders.SelectBuilder;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.internal.value_objects.CTEEntry;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class CTESQLTest {

    private static JQ.Read usersQuery() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("name"))
            .from("users")
            .build();
    }

    private static JQ.Read ordersQuery() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("status"))
            .from("orders")
            .build();
    }

    private static JQ.Read itemsQuery() {
        return SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("product"))
            .from("order_items")
            .build();
    }

    private static JQ.Read finalQuery() {
        return SelectBuilder.select(new ColumnRef.Base("id"))
            .from("users")
            .build();
    }

    private static JQ.Write deleteQuery() {
        return new DeleteBuilder("users").build();
    }

    private static CTEEntry.Regular regular(String name, JQ query) {
        return new CTEEntry.Regular(name, query);
    }

    private static CTEEntry.Recursive recursive(String name, JQ.Read base, JQ.Read rec, UnionType type) {
        return new CTEEntry.Recursive(name, base, rec, type);
    }

    private static CTEEntry.Recursive recursiveAll(String name, JQ.Read base, JQ.Read rec) {
        return recursive(name, base, rec, UnionType.UNION_ALL);
    }

    @Nested
    class RegularEntries {

        @Test
        void singleEntry_readFinal() {
            var result = CTESQL.build(
                List.of(regular("cte", usersQuery())),
                finalQuery()
            );
            assertEquals(
                "WITH cte AS (SELECT id, name FROM users) SELECT id FROM users",
                result
            );
        }

        @Test
        void singleEntry_writeFinal() {
            var result = CTESQL.build(
                List.of(regular("cte", usersQuery())),
                deleteQuery()
            );
            assertEquals(
                "WITH cte AS (SELECT id, name FROM users) DELETE FROM users",
                result
            );
        }

        @Test
        void multipleEntries_commaSeparated() {
            var result = CTESQL.build(
                List.of(
                    regular("a", usersQuery()),
                    regular("b", ordersQuery()),
                    regular("c", itemsQuery())
                ),
                finalQuery()
            );
            assertEquals(
                "WITH a AS (SELECT id, name FROM users), " +
                "b AS (SELECT id, status FROM orders), " +
                "c AS (SELECT id, product FROM order_items) " +
                "SELECT id FROM users",
                result
            );
        }

        @Test
        void noRecursiveKeyword_withOnlyRegular() {
            var result = CTESQL.build(
                List.of(regular("a", usersQuery()), regular("b", ordersQuery())),
                finalQuery()
            );
            assertFalse(result.contains("RECURSIVE"));
        }

        @Test
        void entryNamePreserved() {
            var result = CTESQL.build(
                List.of(regular("my_cte_name", usersQuery())),
                finalQuery()
            );
            assertTrue(result.contains("my_cte_name AS (SELECT id, name FROM users)"));
        }

        @Test
        void startsWithWith() {
            var result = CTESQL.build(
                List.of(regular("cte", usersQuery())),
                finalQuery()
            );
            assertTrue(result.startsWith("WITH "));
        }

        @Test
        void finalQueryAppendsAtEnd() {
            var result = CTESQL.build(
                List.of(regular("cte", usersQuery())),
                finalQuery()
            );
            assertTrue(result.endsWith("SELECT id FROM users"));
        }
    }

    @Nested
    class RecursiveEntries {

        @Test
        void recursive_unionAll_exactSql() {
            var result = CTESQL.build(
                List.of(recursiveAll("tree", usersQuery(), ordersQuery())),
                finalQuery()
            );
            assertEquals(
                "WITH RECURSIVE tree AS (" +
                "SELECT id, name FROM users " +
                "UNION ALL " +
                "SELECT id, status FROM orders" +
                ") SELECT id FROM users",
                result
            );
        }

        @Test
        void recursive_union() {
            var result = CTESQL.build(
                List.of(recursive("tree", usersQuery(), ordersQuery(), UnionType.UNION)),
                finalQuery()
            );
            assertTrue(result.contains("tree AS (SELECT id, name FROM users UNION SELECT id, status FROM orders)"));
            assertFalse(result.contains("UNION ALL"));
        }

        @Test
        void recursive_intersect() {
            var result = CTESQL.build(
                List.of(recursive("tree", usersQuery(), ordersQuery(), UnionType.INTERSECT)),
                finalQuery()
            );
            assertTrue(result.contains("tree AS (SELECT id, name FROM users INTERSECT SELECT id, status FROM orders)"));
        }

        @Test
        void recursive_except() {
            var result = CTESQL.build(
                List.of(recursive("tree", usersQuery(), ordersQuery(), UnionType.EXCEPT)),
                finalQuery()
            );
            assertTrue(result.contains("tree AS (SELECT id, name FROM users EXCEPT SELECT id, status FROM orders)"));
        }

        @Test
        void recursiveKeyword_withOnlyRecursive() {
            var result = CTESQL.build(
                List.of(recursiveAll("tree", usersQuery(), ordersQuery())),
                finalQuery()
            );
            assertTrue(result.startsWith("WITH RECURSIVE"));
        }

        @Test
        void recursiveKeyword_mixedWithRegular() {
            var result = CTESQL.build(
                List.of(
                    regular("reg", usersQuery()),
                    recursiveAll("tree", usersQuery(), ordersQuery())
                ),
                finalQuery()
            );
            assertTrue(result.startsWith("WITH RECURSIVE"));
            assertTrue(result.contains("reg AS (SELECT id, name FROM users)"));
            assertTrue(result.contains("tree AS (SELECT id, name FROM users UNION ALL SELECT id, status FROM orders)"));
        }

        @Test
        void multipleRecursive_allRendered() {
            var result = CTESQL.build(
                List.of(
                    recursiveAll("a", usersQuery(), ordersQuery()),
                    recursive("b", ordersQuery(), itemsQuery(), UnionType.UNION)
                ),
                finalQuery()
            );
            assertTrue(result.contains("a AS (SELECT id, name FROM users UNION ALL SELECT id, status FROM orders)"));
            assertTrue(result.contains("b AS (SELECT id, status FROM orders UNION SELECT id, product FROM order_items)"));
        }
    }

    @Nested
    class Ordering {

        @Test
        void entriesRenderedInOrder() {
            var result = CTESQL.build(
                List.of(
                    regular("first",  usersQuery()),
                    regular("second", ordersQuery()),
                    regular("third",  itemsQuery())
                ),
                finalQuery()
            );
            assertTrue(result.indexOf("first") < result.indexOf("second"));
            assertTrue(result.indexOf("second") < result.indexOf("third"));
        }
    }
}