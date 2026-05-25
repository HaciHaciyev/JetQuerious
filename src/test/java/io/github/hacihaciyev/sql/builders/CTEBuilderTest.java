package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class CTEBuilderTest {

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

    @Nested
    class Regular {

        @Test
        void singleCte_readFinal() {
            var jq = new CTEBuilder("active_users", usersQuery())
                .build(finalQuery());

            assertInstanceOf(JQ.Read.class, jq);
            assertEquals(
                "WITH active_users AS (SELECT id, name FROM users) SELECT id FROM users",
                jq.sql()
            );
        }

        @Test
        void singleCte_writeFinal() {
            var jq = new CTEBuilder("active_users", usersQuery())
                .build(deleteQuery());

            assertInstanceOf(JQ.Write.class, jq);
            assertEquals(
                "WITH active_users AS (SELECT id, name FROM users) DELETE FROM users",
                jq.sql()
            );
        }

        @Test
        void multipleCtes_correctOrder() {
            var jq = new CTEBuilder("a", usersQuery())
                .with("b", ordersQuery())
                .with("c", itemsQuery())
                .build(finalQuery());

            assertEquals(
                "WITH a AS (SELECT id, name FROM users), " +
                "b AS (SELECT id, status FROM orders), " +
                "c AS (SELECT id, product FROM order_items) " +
                "SELECT id FROM users",
                jq.sql()
            );
        }

        @Test
        void cteWithDmlQuery() {
            var jq = new CTEBuilder("cte", usersQuery())
                .build(new DeleteBuilder("users")
                    .where(new BinaryOp(BinaryOp.BinaryOperator.EQ,
                        new ColumnRef.Base("id"), new Literal.IntLiteral(1)))
                    .build());

            assertEquals(
                "WITH cte AS (SELECT id, name FROM users) DELETE FROM users WHERE (id = 1)",
                jq.sql()
            );
        }

        @Test
        void noRecursiveKeyword_whenNoRecursiveCte() {
            var jq = new CTEBuilder("a", usersQuery())
                .with("b", ordersQuery())
                .build(finalQuery());

            assertFalse(jq.sql().contains("RECURSIVE"));
        }

        @Test
        void cteWithSubquery() {
            var sub = SelectBuilder.select(new ColumnRef.Base("id"))
                .from("orders")
                .where(new BinaryOp(BinaryOp.BinaryOperator.GT,
                    new ColumnRef.Base("amount"), new Literal.IntLiteral(100)))
                .build();

            var jq = new CTEBuilder("big_orders", sub)
                .build(finalQuery());

            assertEquals(
                "WITH big_orders AS (SELECT id FROM orders WHERE (amount > 100)) SELECT id FROM users",
                jq.sql()
            );
        }
    }

    @Nested
    class Recursive {

        @Test
        void recursive_defaultUnionAll() {
            var jq = new CTEBuilder("tree", usersQuery())
                .withRecursive("subtree", usersQuery(), ordersQuery())
                .build(finalQuery());

            assertTrue(jq.sql().contains("WITH RECURSIVE"));
            assertTrue(jq.sql().contains("UNION ALL"));
        }

        @Test
        void recursive_unionAll_exactSql() {
            var jq = new CTEBuilder("tree", usersQuery())
                .withRecursive("subtree", usersQuery(), ordersQuery(), UnionType.UNION_ALL)
                .build(finalQuery());

            assertTrue(jq.sql().contains(
                "subtree AS (SELECT id, name FROM users UNION ALL SELECT id, status FROM orders)"
            ));
        }

        @Test
        void recursive_union() {
            var jq = new CTEBuilder("tree", usersQuery())
                .withRecursive("subtree", usersQuery(), ordersQuery(), UnionType.UNION)
                .build(finalQuery());

            assertTrue(jq.sql().contains("WITH RECURSIVE"));
            assertTrue(jq.sql().contains(
                "subtree AS (SELECT id, name FROM users UNION SELECT id, status FROM orders)"
            ));
            assertFalse(jq.sql().contains("UNION ALL"));
        }

        @Test
        void recursive_intersect() {
            var jq = new CTEBuilder("tree", usersQuery())
                .withRecursive("subtree", usersQuery(), ordersQuery(), UnionType.INTERSECT)
                .build(finalQuery());

            assertTrue(jq.sql().contains("WITH RECURSIVE"));
            assertTrue(jq.sql().contains(
                "subtree AS (SELECT id, name FROM users INTERSECT SELECT id, status FROM orders)"
            ));
        }

        @Test
        void recursive_except() {
            var jq = new CTEBuilder("tree", usersQuery())
                .withRecursive("subtree", usersQuery(), ordersQuery(), UnionType.EXCEPT)
                .build(finalQuery());

            assertTrue(jq.sql().contains("WITH RECURSIVE"));
            assertTrue(jq.sql().contains(
                "subtree AS (SELECT id, name FROM users EXCEPT SELECT id, status FROM orders)"
            ));
        }

        @Test
        void onlyRecursive_startsWithRecursive() {
            var jq = new CTEBuilder("tree", usersQuery())
                .withRecursive("subtree", usersQuery(), ordersQuery())
                .build(finalQuery());

            assertTrue(jq.sql().startsWith("WITH RECURSIVE"));
        }

        @Test
        void mixedRegularAndRecursive_hasRecursiveKeyword() {
            var jq = new CTEBuilder("regular", usersQuery())
                .with("also_regular", ordersQuery())
                .withRecursive("tree", usersQuery(), ordersQuery())
                .build(finalQuery());

            assertTrue(jq.sql().startsWith("WITH RECURSIVE"));
            assertTrue(jq.sql().contains("regular AS"));
            assertTrue(jq.sql().contains("also_regular AS"));
            assertTrue(jq.sql().contains("tree AS"));
        }

        @Test
        void multipleRecursive_allPresent() {
            var jq = new CTEBuilder("x", usersQuery())
                .withRecursive("a", usersQuery(), ordersQuery())
                .withRecursive("b", ordersQuery(), itemsQuery())
                .build(finalQuery());

            assertTrue(jq.sql().contains("WITH RECURSIVE"));
            assertTrue(jq.sql().contains("a AS"));
            assertTrue(jq.sql().contains("b AS"));
        }
    }

    @Nested
    class Immutability {

        @Test
        void with_doesNotMutateOriginal() {
            var builder  = new CTEBuilder("cte1", usersQuery());
            var extended = builder.with("cte2", ordersQuery());

            assertFalse(builder.build(finalQuery()).sql().contains("cte2"));
            assertTrue(extended.build(finalQuery()).sql().contains("cte2"));
        }

        @Test
        void withRecursive_doesNotMutateOriginal() {
            var builder  = new CTEBuilder("cte1", usersQuery());
            var extended = builder.withRecursive("tree", usersQuery(), ordersQuery());

            assertFalse(builder.build(finalQuery()).sql().contains("RECURSIVE"));
            assertTrue(extended.build(finalQuery()).sql().contains("RECURSIVE"));
        }

        @Test
        void chainedWith_eachStepIndependent() {
            var b1 = new CTEBuilder("a", usersQuery());
            var b2 = b1.with("b", ordersQuery());
            var b3 = b2.with("c", itemsQuery());

            assertFalse(b1.build(finalQuery()).sql().contains("b AS"));
            assertFalse(b2.build(finalQuery()).sql().contains("c AS"));
            assertTrue(b3.build(finalQuery()).sql().contains("c AS"));
        }
    }

    @Nested
    class ContextPassthrough {

        @Test
        void read_contextIsFromFinalQuery() {
            var final_ = finalQuery();
            var jq     = new CTEBuilder("cte", usersQuery()).build(final_);

            assertSame(final_.context(), jq.context());
        }

        @Test
        void write_contextIsFromFinalQuery() {
            var final_ = deleteQuery();
            var jq     = new CTEBuilder("cte", usersQuery()).build(final_);

            assertSame(final_.context(), jq.context());
        }
    }

    @Nested
    class Validation {

        @Test
        void nullName_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder(null, usersQuery()));
        }

        @Test
        void blankName_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("  ", usersQuery()));
        }

        @Test
        void nullQuery_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("name", null));
        }

        @Test
        void nullWith_name_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).with(null, ordersQuery()));
        }

        @Test
        void blankWith_name_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).with("  ", ordersQuery()));
        }

        @Test
        void nullWith_query_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).with("b", null));
        }

        @Test
        void nullRecursive_name_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).withRecursive(null, usersQuery(), ordersQuery()));
        }

        @Test
        void blankRecursive_name_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).withRecursive("  ", usersQuery(), ordersQuery()));
        }

        @Test
        void nullRecursive_base_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).withRecursive("b", null, ordersQuery()));
        }

        @Test
        void nullRecursive_recursive_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).withRecursive("b", usersQuery(), null));
        }

        @Test
        void nullRecursive_unionType_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                new CTEBuilder("a", usersQuery()).withRecursive("b", usersQuery(), ordersQuery(), null));
        }

        @Test
        void nullFinalReadQuery_throws() {
            assertThrows(NullPointerException.class, () ->
                new CTEBuilder("a", usersQuery()).build((JQ.Read) null));
        }

        @Test
        void nullFinalWriteQuery_throws() {
            assertThrows(NullPointerException.class, () ->
                new CTEBuilder("a", usersQuery()).build((JQ.Write) null));
        }
    }
    
    @Nested
    class DuplicateNameValidation {
    
        @Test
        void with_duplicateName_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CTEBuilder("cte", usersQuery())
                            .with("cte", ordersQuery())
            );
        }
    
        @Test
        void with_duplicateNameCaseInsensitive_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CTEBuilder("CTE", usersQuery())
                            .with("cte", ordersQuery())
            );
        }
    
        @Test
        void withRecursive_duplicateName_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CTEBuilder("tree", usersQuery())
                            .withRecursive("tree", usersQuery(), ordersQuery())
            );
        }
    
        @Test
        void withRecursive_duplicatesExistingRegular_throws() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CTEBuilder("cte", usersQuery())
                            .with("other", ordersQuery())
                            .withRecursive("cte", usersQuery(), ordersQuery())
            );
        }
    
        @Test
        void with_differentNames_passes() {
            assertDoesNotThrow(() ->
                    new CTEBuilder("cte1", usersQuery())
                            .with("cte2", ordersQuery())
                            .build(finalQuery())
            );
        }
    
        @Test
        void withRecursive_differentNameFromExisting_passes() {
            assertDoesNotThrow(() ->
                    new CTEBuilder("cte", usersQuery())
                            .withRecursive("tree", usersQuery(), ordersQuery())
                            .build(finalQuery())
            );
        }
    
        @Test
        void with_threeDistinctNames_passes() {
            var itemsQuery = SelectBuilder.select(new ColumnRef.Base("id"), new ColumnRef.Base("product"))
                    .from("order_items")
                    .build();
    
            assertDoesNotThrow(() ->
                    new CTEBuilder("a", usersQuery())
                            .with("b", ordersQuery())
                            .with("c", itemsQuery)
                            .build(finalQuery())
            );
        }
    }
}