package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.builders.TransactionBuilder;
import io.github.hacihaciyev.util.DBTestContainer;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.sql.Connection;

import java.util.concurrent.atomic.AtomicLong;

import static io.github.hacihaciyev.sql.QueryForge.*;
import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class BoundTransactionExecutionTest {

    private static final AtomicLong ID_SEQ = new AtomicLong(10_000);

    private JetQuerious jq;

    @BeforeEach
    void setUp() {
        jq = JetQuerious.defaultInstance();
        jq.write(deleteFrom("order_items").build());
        jq.write(deleteFrom("orders").build());
        jq.write(deleteFrom("banned_users").build());
        jq.write(deleteFrom("users").build());
        jq.write(deleteFrom("employees").build());
    }

    private static long nextId() {
        return ID_SEQ.getAndIncrement();
    }


    private static io.github.hacihaciyev.sql.JQ.Write insertUser() {
        return insertInto("users")
            .columns("id", Long.class, "name", String.class, "email", String.class, "active", Boolean.class)
            .build();
    }

    private static io.github.hacihaciyev.sql.JQ.Write insertOrder() {
        return insertInto("orders")
            .columns("id", Long.class, "user_id", Long.class, "total", BigDecimal.class,
                "amount", BigDecimal.class, "status", String.class)
            .build();
    }

    private static io.github.hacihaciyev.sql.JQ.Write updateOrderTotal() {
        return update("orders")
            .set("total", BigDecimal.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static io.github.hacihaciyev.sql.JQ.Write incrementOrderTotal() {
        return update("orders")
            .set("total", add(col("total"), param(BigDecimal.class)))
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static io.github.hacihaciyev.sql.JQ.Write insertBannedUser() {
        return insertInto("banned_users")
            .columns("id", Long.class, "user_id", Long.class)
            .build();
    }

    private static io.github.hacihaciyev.sql.JQ.Read selectOrderTotal() {
        return select(col("total"))
            .from("orders")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }


    private static io.github.hacihaciyev.sql.JQ.Read countBannedUsers() {
        return select(countAll())
            .from("banned_users")
            .build();
    }

    private static ResultSetExtractor<BigDecimal> totalExtractor() {
        return rs -> rs.getBigDecimal("total");
    }

    private static ResultSetExtractor<Long> countExtractor() {
        return rs -> rs.getLong("count");
    }

    private long insertTestUser(String name, String email) {
        var id = nextId();
        var result = jq.write(insertUser(), id, name, email, true);
        assertInstanceOf(Ok.class, result);
        return id;
    }

    private long insertTestOrder(long userId, BigDecimal total) {
        var id = nextId();
        var result = jq.write(insertOrder(), id, userId, total, total, "pending");
        assertInstanceOf(Ok.class, result);
        return id;
    }

    @Test
    void execute_commitsAllWrites() {
        var userId = insertTestUser("Alice", "alice@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("10.00"));

        var tx = transaction()
            .add(updateOrderTotal())
            .add(insertBannedUser())
            .build();

        var result = jq.transaction(tx)
            .bind(new BigDecimal("25.50"), orderId)
            .bind(nextId(), userId)
            .execute();

        assertInstanceOf(Ok.class, result);
        assertArrayEquals(new int[]{1, 1}, result.or(new int[0]));

        var totalResult = jq.one(selectOrderTotal(), totalExtractor(), orderId);
        assertInstanceOf(Ok.class, totalResult);
        assertEquals(0, new BigDecimal("25.50").compareTo(totalResult.or(BigDecimal.ZERO)));

        var bannedCount = jq.one(countBannedUsers(), countExtractor());
        assertInstanceOf(Ok.class, bannedCount);
        assertEquals(1L, bannedCount.or(0L));
    }

    @Test
    void execute_rollbackOnFailure_undoesEarlierWrites() {
        var userId = insertTestUser("Bob", "bob@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("40.00"));

        var twoParamInsert = insertInto("banned_users")
            .columns("id", Long.class, "user_id", Long.class)
            .build();

        var tx = transaction()
            .add(updateOrderTotal())
            .savepoint("after_update")
            .add(twoParamInsert)
            .build();

        var result = jq.transaction(tx)
            .bind(new BigDecimal("99.99"), orderId)
            .bind(userId)
            .execute();

        assertInstanceOf(Err.class, result);

        var totalResult = jq.one(selectOrderTotal(), totalExtractor(), orderId);
        assertInstanceOf(Ok.class, totalResult);
        assertEquals(0, new BigDecimal("40.00").compareTo(totalResult.or(BigDecimal.ZERO)));

        var bannedCount = jq.one(countBannedUsers(), countExtractor());
        assertInstanceOf(Ok.class, bannedCount);
        assertEquals(0L, bannedCount.or(-1L));
    }

    @Test
    void execute_countsBindCallsPerOperation_notPerParameter() {
        var userId = insertTestUser("Carol", "carol@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("20.00"));

        var tx = transaction()
            .add(incrementOrderTotal())
            .add(updateOrderTotal())
            .build();

        var result = jq.transaction(tx)
            .bind(new BigDecimal("5.00"), orderId)
            .bind(new BigDecimal("17.00"), orderId)
            .execute();

        assertInstanceOf(Ok.class, result);
        assertArrayEquals(new int[]{1, 1}, result.or(new int[0]));

        var totalResult = jq.one(selectOrderTotal(), totalExtractor(), orderId);
        assertInstanceOf(Ok.class, totalResult);
        assertEquals(0, new BigDecimal("17.00").compareTo(totalResult.or(BigDecimal.ZERO)));
    }

    @Test
    void execute_rejectsTooFewBindCalls() {
        var userId = insertTestUser("Dave", "dave@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("11.00"));

        var tx = transaction()
            .add(updateOrderTotal())
            .add(insertBannedUser())
            .build();

        var result = jq.transaction(tx)
            .bind(new BigDecimal("12.00"), orderId)
            .execute();

        assertInstanceOf(Err.class, result);
        var err = (Err<int[], Exception>) result;
        assertTrue(err.err() instanceof IllegalArgumentException);
        assertTrue(err.err().getMessage().contains("Expected 2 bind() calls but got 1"));
    }

    @Test
    void execute_rejectsTooManyBindCalls() {
        var userId = insertTestUser("Eve", "eve@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("13.00"));

        var tx = transaction()
            .add(updateOrderTotal())
            .build();

        var result = jq.transaction(tx)
            .bind(new BigDecimal("14.00"), orderId)
            .bind(nextId(), userId)
            .execute();

        assertInstanceOf(Err.class, result);
        var err = (Err<int[], Exception>) result;
        assertTrue(err.err() instanceof IllegalArgumentException);
        assertTrue(err.err().getMessage().contains("Expected 1 bind() calls but got 2"));
    }

    @Test
    void execute_mixedReadAndWrite_consumesBindingsInOperationOrder() {
        var userId = insertTestUser("Frank", "frank@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("30.00"));

        var tx = transaction()
            .add(selectOrderTotal())
            .add(updateOrderTotal())
            .add(selectOrderTotal())
            .build();

        var result = jq.transaction(tx)
            .bind(orderId)
            .bind(new BigDecimal("44.00"), orderId)
            .bind(orderId)
            .execute();

        assertInstanceOf(Ok.class, result);
        assertArrayEquals(new int[]{0, 1, 0}, result.or(new int[0]));

        var totalResult = jq.one(selectOrderTotal(), totalExtractor(), orderId);
        assertInstanceOf(Ok.class, totalResult);
        assertEquals(0, new BigDecimal("44.00").compareTo(totalResult.or(BigDecimal.ZERO)));
    }

    @Test
    void execute_allowsSavepointsAtStartMiddleAndEnd() {
        var userId = insertTestUser("Grace", "grace@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("55.00"));

        var tx = transaction()
            .savepoint("before_anything")
            .add(updateOrderTotal())
            .savepoint("after_update")
            .add(selectOrderTotal())
            .savepoint("after_read")
            .build();

        var result = jq.transaction(tx)
            .bind(new BigDecimal("60.00"), orderId)
            .bind(orderId)
            .execute();

        assertInstanceOf(Ok.class, result);
        assertArrayEquals(new int[]{1, 0}, result.or(new int[0]));
    }

    @Test
    void execute_restoresIsolationLevelBeforeConnectionIsClosed() throws Exception {
        var userId = insertTestUser("Heidi", "heidi@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("70.00"));

        final int[] isolationSeenOnClose = {Integer.MIN_VALUE};

        var base = io.github.hacihaciyev.config.Conf.INSTANCE.dataSource().orElseThrow();
        javax.sql.DataSource observing = new javax.sql.DataSource() {
            @Override
            public Connection getConnection() throws java.sql.SQLException {
                return wrap(base.getConnection());
            }

            @Override
            public Connection getConnection(String username, String password) throws java.sql.SQLException {
                return wrap(base.getConnection(username, password));
            }

            private Connection wrap(Connection delegate) {
                return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName())) {
                            isolationSeenOnClose[0] = delegate.getTransactionIsolation();
                        }
                        return method.invoke(delegate, args);
                    }
                );
            }

            @Override public java.io.PrintWriter getLogWriter() throws java.sql.SQLException { return base.getLogWriter(); }
            @Override public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException { base.setLogWriter(out); }
            @Override public void setLoginTimeout(int seconds) throws java.sql.SQLException { base.setLoginTimeout(seconds); }
            @Override public int getLoginTimeout() throws java.sql.SQLException { return base.getLoginTimeout(); }
            @Override public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException { return base.getParentLogger(); }
            @Override public <T> T unwrap(Class<T> iface) throws java.sql.SQLException { return base.unwrap(iface); }
            @Override public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException { return base.isWrapperFor(iface); }
        };

        int baseline;
        try (var conn = base.getConnection()) {
            baseline = conn.getTransactionIsolation();
        }

        var tx = new TransactionBuilder()
            .add(updateOrderTotal())
            .isolationLevel(io.github.hacihaciyev.sql.Transaction.IsolationLevel.SERIALIZABLE)
            .build();

        var result = new JetQuerious(observing)
            .transaction(tx)
            .bind(new BigDecimal("71.00"), orderId)
            .execute();

        assertInstanceOf(Ok.class, result);
        assertEquals(baseline, isolationSeenOnClose[0]);
    }

    @Test
    void bind_copiesArgumentArrayDefensively() {
        var userId = insertTestUser("Ivan", "ivan@example.com");
        var orderId = insertTestOrder(userId, new BigDecimal("80.00"));

        var args = new Object[]{new BigDecimal("81.00"), orderId};
        var tx = transaction().add(updateOrderTotal()).build();
        var bound = jq.transaction(tx).bind(args);
        args[0] = new BigDecimal("999.99");

        var result = bound.execute();

        assertInstanceOf(Ok.class, result);

        var totalResult = jq.one(selectOrderTotal(), totalExtractor(), orderId);
        assertInstanceOf(Ok.class, totalResult);
        assertEquals(0, new BigDecimal("81.00").compareTo(totalResult.or(BigDecimal.ZERO)));
    }
}
