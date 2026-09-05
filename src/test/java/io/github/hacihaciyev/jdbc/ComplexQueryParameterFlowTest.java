package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import io.github.hacihaciyev.util.Ok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.hacihaciyev.sql.QueryForge.*;
import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class ComplexQueryParameterFlowTest {

    private static final AtomicLong ID_SEQ = new AtomicLong(20_000);

    private JetQuerious jq;

    @BeforeEach
    void setUp() {
        jq = JetQuerious.defaultInstance();
        jq.write(deleteFrom("order_items").build());
        jq.write(deleteFrom("orders").build());
        jq.write(deleteFrom("users").build());
        jq.write(deleteFrom("employees").build());
        jq.write(deleteFrom("banned_users").build());
    }

    private static long nextId() {
        return ID_SEQ.getAndIncrement();
    }

    private static JQ.Write insertUser() {
        return insertInto("users")
            .columns("id", Long.class, "name", String.class, "email", String.class, "active", Boolean.class)
            .build();
    }

    private static JQ.Write insertOrder() {
        return insertInto("orders")
            .columns("id", Long.class, "user_id", Long.class, "total", BigDecimal.class,
                "amount", BigDecimal.class, "status", String.class)
            .build();
    }

    private static ResultSetExtractor<Long> longValue(String column) {
        return rs -> rs.getLong(column);
    }

    private static ResultSetExtractor<String> stringValue(String column) {
        return rs -> rs.getString(column);
    }


    private long insertTestUser(String name, String email, boolean active) {
        var id = nextId();
        var result = jq.write(insertUser(), id, name, email, active);
        assertInstanceOf(Ok.class, result);
        return id;
    }

    private long insertTestOrder(long userId, BigDecimal total, String status) {
        var id = nextId();
        var result = jq.write(insertOrder(), id, userId, total, total, status);
        assertInstanceOf(Ok.class, result);
        return id;
    }

    @Test
    void cteWithParameterizedEntryAndParameterizedFinalQuery_declaresBothButExecutionStillNeedsMatchingSemantics() {
        var activeUser = insertTestUser("Alice", "alice@example.com", true);
        var inactiveUser = insertTestUser("Bob", "bob@example.com", false);
        insertTestOrder(activeUser, new BigDecimal("120.00"), "pending");
        insertTestOrder(activeUser, new BigDecimal("50.00"), "closed");
        insertTestOrder(inactiveUser, new BigDecimal("200.00"), "pending");

        var cteEntry = select(col("user_id"))
            .from("orders")
            .where(and(
                eq(col("status"), param(String.class)),
                gt(col("total"), param(BigDecimal.class))
            ))
            .build();

        var cte = with("picked_users", cteEntry).build(
            select(col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build()
        );

        var result = jq.many(
            cte,
            stringValue("name"),
            "pending",
            new BigDecimal("100.00"),
            true
        );

        assertInstanceOf(Ok.class, result);
        assertEquals(List.of("Alice"), result.or(List.of()));
    }

    @Test
    void outerContextBuilderAndStandaloneCorrelatedSubqueryEachKeepOwnParameters() {
        var alice = insertTestUser("Alice", "alice@example.com", true);
        var bob = insertTestUser("Bob", "bob@example.com", true);
        insertTestOrder(alice, new BigDecimal("90.00"), "pending");
        insertTestOrder(alice, new BigDecimal("15.00"), "cancelled");
        insertTestOrder(bob, new BigDecimal("140.00"), "pending");

        var outer = select(col("u", "name"), col("u", "id"))
            .from(new TableRef.AliasedBase("users", "u"))
            .where(eq(col("u", "active"), param(Boolean.class)))
            .build();

        var outerResult = jq.many(outer, stringValue("name"), true);
        assertInstanceOf(Ok.class, outerResult);
        assertEquals(List.of("Alice", "Bob"), outerResult.or(List.of()));

        var correlated = select(countAll())
            .from("orders")
            .where(and(
                eq(col("user_id"), param(Long.class)),
                eq(col("status"), param(String.class)),
                gt(col("total"), param(BigDecimal.class))
            ))
            .build(outer);

        var countResult = jq.one(correlated, longValue("count"), alice, "pending", new BigDecimal("80.00"));
        assertInstanceOf(Ok.class, countResult);
        assertEquals(1L, countResult.or(0L));
    }

    @Test
    void cteWriteWithParameterizedEntryAndParameterizedDelete_executesInDeclaredOrder() {
        // The CTE entry is not referenced by the final DELETE (self-referencing a CTE by name
        // from a separately-built, schema-validated subquery is not supported by the public
        // builder API). This still proves that CTEWrite.paramTypes() orders entry params before
        // the inner statement's own params, and that JetQuerious binds them in that same order.
        var alice = insertTestUser("Alice", "alice@example.com", true);
        var bob = insertTestUser("Bob", "bob@example.com", true);
        insertTestOrder(alice, new BigDecimal("160.00"), "pending");
        insertTestOrder(bob, new BigDecimal("170.00"), "pending");

        var cteEntry = select(col("id"))
            .from("orders")
            .where(eq(col("status"), param(String.class)))
            .build();

        var delete = deleteFrom("users")
            .where(like(col("name"), param(String.class)))
            .returning("id")
            .build();

        var cteWrite = with("recent_orders", cteEntry).build(delete);

        var result = jq.writeMany(
            cteWrite,
            longValue("id"),
            "pending",
            "A%"
        );

        assertInstanceOf(Ok.class, result);
        assertEquals(List.of(alice), result.or(List.of()));

        var remaining = jq.many(
            select(col("name")).from("users").orderBy("name").build(),
            stringValue("name")
        );
        assertInstanceOf(Ok.class, remaining);
        assertEquals(List.of("Bob"), remaining.or(List.of()));
    }

    @Test
    void writeBatch_reusesStatementAcrossRowsWithoutBleedingParameters() {
        var insertEmployee = insertInto("employees")
            .columns("id", Long.class, "department", String.class, "salary", BigDecimal.class, "active", Boolean.class)
            .build();

        var batch = List.of(
            new Object[]{nextId(), "engineering", new BigDecimal("1000.00"), true},
            new Object[]{nextId(), "sales", new BigDecimal("2000.00"), false},
            new Object[]{nextId(), "support", new BigDecimal("3000.00"), true}
        );

        var result = jq.writeBatch(insertEmployee, batch);

        assertInstanceOf(Ok.class, result);
        assertArrayEquals(new int[]{1, 1, 1}, result.or(new int[0]));

        var query = select(col("department"))
            .from("employees")
            .where(eq(col("salary"), param(BigDecimal.class)))
            .build();

        var first = jq.one(query, stringValue("department"), new BigDecimal("1000.00"));
        var second = jq.one(query, stringValue("department"), new BigDecimal("2000.00"));
        var third = jq.one(query, stringValue("department"), new BigDecimal("3000.00"));

        assertEquals("engineering", first.or((String) null));
        assertEquals("sales", second.or((String) null));
        assertEquals("support", third.or((String) null));
    }

    @Test
    void option_returnsEmptyWhenNoRowsInComplexParameterizedQuery() {
        var userId = insertTestUser("Mila", "mila@example.com", true);
        insertTestOrder(userId, new BigDecimal("10.00"), "closed");

        var qualifyingOrders = select(col("user_id"))
            .from("orders")
            .where(and(
                eq(col("status"), param(String.class)),
                gt(col("total"), param(BigDecimal.class))
            ))
            .build();

        var query = with("qualifying", qualifyingOrders).build(
            select(col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build()
        );

        var result = jq.option(query, stringValue("name"), "pending", new BigDecimal("1000.00"), false);

        assertInstanceOf(Ok.class, result);
        assertEquals(Optional.empty(), result.or(Optional.of("unexpected")));
    }
}
