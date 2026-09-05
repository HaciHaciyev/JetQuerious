package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.util.DBTestContainer;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import io.github.hacihaciyev.sql.builders.CTEBuilder;
import io.github.hacihaciyev.sql.builders.DeleteBuilder;
import io.github.hacihaciyev.sql.builders.SelectBuilder;
import io.github.hacihaciyev.sql.builders.UnionBuilder;
import io.github.hacihaciyev.sql.expressions.InExpr;
import io.github.hacihaciyev.sql.expressions.Subquery;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.sql.value_objects.UnionType;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class JetQueriousTest {

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    private JetQuerious jq;

    @BeforeEach
    void setUp() {
        jq = JetQuerious.defaultInstance();
        jq.write(deleteFrom("order_items").build());
        jq.write(deleteFrom("orders").build());
        jq.write(deleteFrom("users").build());
        jq.write(deleteFrom("employees").build());
    }

    private static long nextId() {
        return ID_SEQ.getAndIncrement();
    }


    private static JQ.Write insertUser() {
        return insertInto("users")
            .columns("id", Long.class, "name", String.class, "email", String.class, "active", Boolean.class)
            .build();
    }

    private static JQ.Write insertUserReturningId() {
        return insertInto("users")
            .columns("id", Long.class, "name", String.class, "email", String.class, "active", Boolean.class)
            .returning(col("id"))
            .build();
    }

    private static JQ.Write updateUserName() {
        return update("users")
            .set("name", String.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Write updateUserActive() {
        return update("users")
            .set("active", Boolean.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Write deleteUser() {
        return deleteFrom("users")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Write deleteUserReturningId() {
        return deleteFrom("users")
            .where(eq(col("id"), param(Long.class)))
            .returning(col("id"))
            .build();
    }

    private static JQ.Read selectUserById() {
        return select(col("id"), col("name"), col("email"), col("active"))
            .from("users")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Read selectAllUsers() {
        return select(col("id"), col("name"), col("email"), col("active"))
            .from("users")
            .build();
    }

    private static JQ.Read selectUserByEmail() {
        return select(col("id"), col("name"), col("email"))
            .from("users")
            .where(eq(col("email"), param(String.class)))
            .build();
    }

    private static JQ.Read selectUserCount() {
        return select(countAll())
            .from("users")
            .build();
    }

    private static JQ.Write insertOrder() {
        return insertInto("orders")
            .columns("id", Long.class, "user_id", Long.class, "total", BigDecimal.class,
                     "amount", BigDecimal.class, "status", String.class)
            .build();
    }

    private static JQ.Write updateOrderTotal() {
        return update("orders")
            .set("total", BigDecimal.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Write updateOrderTotalReturning() {
        return update("orders")
            .set("total", BigDecimal.class)
            .where(eq(col("id"), param(Long.class)))
            .returning(col("total"))
            .build();
    }

    record UserRow(long id, String name, String email, boolean active) {}
    record UserPartial(long id, String name, String email) {}
    record IdName(long id, String name) {}

    private static ResultSetExtractor<Long>        longId()      { return rs -> rs.getLong("id"); }
    private static ResultSetExtractor<BigDecimal>  decimal()     { return rs -> rs.getBigDecimal("total"); }
    private static ResultSetExtractor<UserRow>     userRow()     { return rs -> new UserRow(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getBoolean("active")); }
    private static ResultSetExtractor<UserPartial> userPartial() { return rs -> new UserPartial(rs.getLong("id"), rs.getString("name"), rs.getString("email")); }
    private static ResultSetExtractor<IdName>      idName()      { return rs -> new IdName(rs.getLong("id"), rs.getString("name")); }

    private long insertTestUser(String name, String email) {
        var id = nextId();
        jq.write(insertUser(), id, name, email, true);
        return id;
    }

    private long insertTestUser(String name, String email, boolean active) {
        var id = nextId();
        jq.write(insertUser(), id, name, email, active);
        return id;
    }

    private long insertTestOrder(long userId, BigDecimal total) {
        var id = nextId();
        jq.write(insertOrder(), id, userId, total, total, "pending");
        return id;
    }

    @Nested
    class Write {

        @Test
        void insertsRow_returnsOne() {
            var result = jq.write(insertUser(), nextId(), "Alice", "alice@example.com", true);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(0));
        }

        @Test
        void updatesExistingRow_returnsOne() {
            var id = insertTestUser("Bob", "bob@example.com");

            var result = jq.write(updateUserName(), "Bobby", id);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(0));
        }

        @Test
        void updatesNonExistentRow_returnsZero() {
            var result = jq.write(updateUserName(), "Ghost", 999999L);

            assertInstanceOf(Ok.class, result);
            assertEquals(0, result.or(-1));
        }

        @Test
        void deletesExistingRow_returnsOne() {
            var id = insertTestUser("Carol", "carol@example.com");

            var result = jq.write(deleteUser(), id);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(0));
        }

        @Test
        void deletesNonExistentRow_returnsZero() {
            var result = jq.write(deleteUser(), 999999L);

            assertInstanceOf(Ok.class, result);
            assertEquals(0, result.or(-1));
        }

        @Test
        void noParams_works() {
            insertTestUser("Dave", "dave@example.com");

            var deleteAll = deleteFrom("users")
                .where(eq(col("active"), lit(true)))
                .build();

            var result = jq.write(deleteAll);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(0));
        }
    }

    @Nested
    class WriteOne {

        @Test
        void insertReturning_returnsId() {
            var id = nextId();

            var result = jq.writeOne(insertUserReturningId(), longId(), id, "Eve", "eve@example.com", true);

            assertInstanceOf(Ok.class, result);
            assertEquals(id, result.or(0L));
        }

        @Test
        void updateReturning_returnsNewValue() {
            var userId  = insertTestUser("Frank", "frank@example.com");
            var orderId = insertTestOrder(userId, new BigDecimal("50.00"));

            var result = jq.writeOne(updateOrderTotalReturning(), decimal(),
                new BigDecimal("99.99"), orderId);

            assertInstanceOf(Ok.class, result);
            assertEquals(0, new BigDecimal("99.99").compareTo(result.or(BigDecimal.ZERO)));
        }

        @Test
        void deleteReturning_returnsDeletedId() {
            var id = insertTestUser("Grace", "grace@example.com");

            var result = jq.writeOne(deleteUserReturningId(), longId(), id);

            assertInstanceOf(Ok.class, result);
            assertEquals(id, result.or(0L));
        }

        @Test
        void noRowsAffected_returnsErr() {
            var result = jq.writeOne(deleteUserReturningId(), longId(), 999999L);

            assertInstanceOf(Err.class, result);
        }
    }

    @Nested
    class WriteOption {

        @Test
        void rowAffected_returnsPresent() {
            var id = insertTestUser("Hank", "hank@example.com");

            var result = jq.writeOption(deleteUserReturningId(), longId(), id);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isPresent());
            assertEquals(id, result.or(Optional.empty()).orElseThrow());
        }

        @Test
        void noRowsAffected_returnsEmpty() {
            var result = jq.writeOption(deleteUserReturningId(), longId(), 999999L);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isEmpty());
        }

        @Test
        void updateReturning_rowExists_returnsPresent() {
            var userId  = insertTestUser("Ida", "ida@example.com");
            var orderId = insertTestOrder(userId, new BigDecimal("100.00"));

            var result = jq.writeOption(updateOrderTotalReturning(), decimal(),
                new BigDecimal("200.00"), orderId);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isPresent());
            assertEquals(0, new BigDecimal("200.00").compareTo(result.or(Optional.empty()).orElseThrow()));
        }

        @Test
        void updateReturning_noRow_returnsEmpty() {
            var result = jq.writeOption(updateOrderTotalReturning(), decimal(),
                new BigDecimal("200.00"), 999999L);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isEmpty());
        }
    }

    @Nested
    class WriteMany {

        @Test
        void deletesMultipleRows_returnsAllIds() {
            insertTestUser("Jack", "jack@example.com");
            insertTestUser("Kate", "kate@example.com");

            var deleteActive = deleteFrom("users")
                .where(eq(col("active"), lit(true)))
                .returning(col("id"))
                .build();

            var result = jq.writeMany(deleteActive, longId());

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }

        @Test
        void noRowsAffected_returnsEmptyList() {
            var result = jq.writeMany(deleteUserReturningId(), longId(), 999999L);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.isOk());
            assertTrue(result instanceof Ok(var value) && value.isEmpty());
        }

        @Test
        void updatesMultiple_returnsUpdatedValues() {
            insertTestUser("Liam", "liam@example.com");
            insertTestUser("Mia", "mia@example.com");

            var updateAll = update("users")
                .set("name", String.class)
                .where(eq(col("active"), lit(true)))
                .returning(col("name"))
                .build();

            var result = jq.writeMany(updateAll, rs -> rs.getString("name"), "Updated");

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
            result.or(List.of()).forEach(name -> assertEquals("Updated", name));
        }

        @Test
        void resultIsImmutable() {
            insertTestUser("Noah", "noah@example.com");

            var deleteActive = deleteFrom("users")
                .where(eq(col("active"), lit(true)))
                .returning(col("id"))
                .build();

            var list = jq.writeMany(deleteActive, longId()).or(List.of());

            assertThrows(UnsupportedOperationException.class, () -> list.add(0L));
        }
    }

    @Nested
    class WriteBatch {

        @Test
        void insertsMultipleRows() {
            var batch = List.of(
                new Object[]{nextId(), "Olivia", "olivia@example.com", true},
                new Object[]{nextId(), "Pete",   "pete@example.com",   true},
                new Object[]{nextId(), "Quinn",  "quinn@example.com",  false}
            );

            var result = jq.writeBatch(insertUser(), batch);

            assertInstanceOf(Ok.class, result);
            assertEquals(3, result.or(new int[0]).length);
            for (var count : result.or(new int[0]))
                assertEquals(1, count);
        }

        @Test
        void updatesMultipleRows() {
            var id1 = insertTestUser("Rita", "rita@example.com");
            var id2 = insertTestUser("Sam",  "sam@example.com");

            var batch = List.of(
                new Object[]{"Rita Updated", id1},
                new Object[]{"Sam Updated",  id2}
            );

            var result = jq.writeBatch(updateUserName(), batch);

            assertInstanceOf(Ok.class, result);
            var counts = result.or(new int[0]);
            assertEquals(2, counts.length);
            assertEquals(1, counts[0]);
            assertEquals(1, counts[1]);
        }

        @Test
        void emptyBatch_returnsEmptyArray() {
            var result = jq.writeBatch(insertUser(), List.of());

            assertInstanceOf(Ok.class, result);
            assertEquals(0, result.or(new int[]{-1}).length);
        }
    }

    @Nested
    class One {

        @Test
        void rowExists_returnsRow() {
            var id = insertTestUser("Tina", "tina@example.com");

            var result = jq.one(selectUserById(), userRow(), id);

            assertInstanceOf(Ok.class, result);
            var user = result.or(() -> { throw new RuntimeException("Unexpected"); });
            assertEquals("Tina", user.name());
            assertEquals("tina@example.com", user.email());
            assertTrue(user.active());
        }

        @Test
        void rowNotFound_returnsErr() {
            var result = jq.one(selectUserById(), userRow(), 999999L);

            assertInstanceOf(Err.class, result);
        }

        @Test
        void withScrollResultSet_returnsRow() {
            var id = insertTestUser("Uma", "uma@example.com");

            var result = jq.one(selectUserById(), userRow(),
                ResultSetType.SCROLL_INSENSITIVE_READ_ONLY, id);

            assertInstanceOf(Ok.class, result);
            assertEquals("Uma", result.or(() -> { throw new RuntimeException("Unexpected"); }).name());
        }

        @Test
        void aggregateCount_returnsValue() {
            insertTestUser("Vera", "vera@example.com");
            insertTestUser("Will", "will@example.com");

            var result = jq.one(selectUserCount(), rs -> rs.getLong(1));

            assertInstanceOf(Ok.class, result);
            assertEquals(2L, result.or(0L));
        }
    }

    @Nested
    class Option {

        @Test
        void rowExists_returnsPresent() {
            var id = insertTestUser("Xena", "xena@example.com");

            var result = jq.option(selectUserById(), userRow(), id);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isPresent());
            assertEquals("Xena", result.or(Optional.empty()).orElseThrow().name());
        }

        @Test
        void rowNotFound_returnsEmpty() {
            var result = jq.option(selectUserById(), userRow(), 999999L);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isEmpty());
        }

        @Test
        void byEmail_returnsPresent() {
            insertTestUser("Yara", "yara@example.com");

            var result = jq.option(selectUserByEmail(), userPartial(), "yara@example.com");

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isPresent());
            assertEquals("Yara", result.or(Optional.empty()).orElseThrow().name());
        }

        @Test
        void withScrollResultSet_returnsPresent() {
            var id = insertTestUser("Zack", "zack@example.com");

            var result = jq.option(selectUserById(), userRow(),
                ResultSetType.SCROLL_INSENSITIVE_READ_ONLY, id);

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isPresent());
        }
    }

    @Nested
    class Many {

        @Test
        void multipleRows_returnsAll() {
            insertTestUser("Alpha", "alpha@example.com");
            insertTestUser("Beta",  "beta@example.com");

            var result = jq.many(selectAllUsers(), userRow());

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }

        @Test
        void noRows_returnsEmptyList() {
            var result = jq.many(selectAllUsers(), userRow());

            assertInstanceOf(Ok.class, result);
            assertTrue(result.isOk());
            assertTrue(result instanceof Ok(var value) && value.isEmpty());
        }

        @Test
        void resultIsImmutable() {
            insertTestUser("Gamma", "gamma@example.com");

            var list = jq.many(selectAllUsers(), userRow()).or(List.of());

            assertThrows(UnsupportedOperationException.class, () -> list.add(null));
        }

        @Test
        void withScrollResultSet_works() {
            insertTestUser("Delta", "delta@example.com");
            insertTestUser("Epsilon", "epsilon@example.com");

            var result = jq.many(selectAllUsers(), userRow(),
                ResultSetType.SCROLL_INSENSITIVE_READ_ONLY);

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }
    }

    @Nested
    class TransactionTests {

        @Test
        void singleWrite_commits() {
            var id = nextId();

            var tx = transaction()
                .add(insertUser())
                .build();

            var result = jq.transaction(tx)
                .bind(id, "Zeta", "zeta@example.com", true)
                .execute();

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(new int[0])[0]);

            assertTrue(jq.option(selectUserById(), userRow(), id).or(Optional.empty()).isPresent());
        }

        @Test
        void multipleWrites_allCommit_dataVerified() {
            var id = insertTestUser("Eta", "eta@example.com");

            var tx = transaction()
                .add(updateUserName())
                .add(updateUserActive())
                .build();

            var result = jq.transaction(tx)
                .bind("Eta Updated", id)
                .bind(false, id)
                .execute();

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(new int[0])[0]);
            assertEquals(1, result.or(new int[0])[1]);

            var user = jq.one(selectUserById(), userRow(), id).or(() -> { throw new RuntimeException("Unexpected"); });
            assertEquals("Eta Updated", user.name());
            assertFalse(user.active());
        }

        @Test
        void withSavepoint_commitsAll() {
            var id1 = nextId();
            var id2 = nextId();

            var tx = transaction()
                .add(insertUser())
                .savepoint("after_first")
                .add(insertUser())
                .build();

            var result = jq.transaction(tx)
                .bind(id1, "Theta", "theta@example.com", true)
                .bind(id2, "Iota",  "iota@example.com",  true)
                .execute();

            assertInstanceOf(Ok.class, result);
            assertTrue(jq.option(selectUserById(), userRow(), id1).or(Optional.empty()).isPresent());
            assertTrue(jq.option(selectUserById(), userRow(), id2).or(Optional.empty()).isPresent());
        }

        @Test
        void operationWithNoParams_skippedInBind() {
            var id = nextId();

            var deleteAll = deleteFrom("users")
                .where(eq(col("active"), lit(false)))
                .build();

            var tx = transaction()
                .add(deleteAll)
                .add(insertUser())
                .build();

            var result = jq.transaction(tx)
                .bind(id, "Kappa", "kappa@example.com", true)
                .execute();

            assertInstanceOf(Ok.class, result);
            assertEquals(0, result.or(new int[0])[0]);
            assertEquals(1, result.or(new int[0])[1]);
        }

        @Test
        void selectForUpdate_thenUpdate() {
            var userId  = insertTestUser("Lambda", "lambda@example.com");
            var orderId = insertTestOrder(userId, new BigDecimal("100.00"));

            var selectForUpdate = select(col("total"))
                .from("orders")
                .where(eq(col("id"), param(Long.class)))
                .forUpdate()
                .build();

            var tx = transaction()
                .add(selectForUpdate)
                .add(updateOrderTotal())
                .isolationLevel(Transaction.IsolationLevel.READ_COMMITTED)
                .build();

            var result = jq.transaction(tx)
                .bind(orderId)
                .bind(new BigDecimal("200.00"), orderId)
                .execute();

            assertInstanceOf(Ok.class, result);
            assertEquals(0, result.or(new int[]{-1})[0]);
            assertEquals(1, result.or(new int[]{-1, -1})[1]);
        }

        @Test
        void transfer_debitAndCredit_bothCommit() {
            var userId  = insertTestUser("Mu", "mu@example.com");
            var fromId  = insertTestOrder(userId, new BigDecimal("500.00"));
            var toId    = insertTestOrder(userId, new BigDecimal("100.00"));

            var debit  = update("orders")
                .set("total", subtract(col("total"), param(BigDecimal.class)))
                .where(eq(col("id"), param(Long.class)))
                .build();

            var credit = update("orders")
                .set("total", add(col("total"), param(BigDecimal.class)))
                .where(eq(col("id"), param(Long.class)))
                .build();

            var tx = transaction()
                .add(debit)
                .add(credit)
                .isolationLevel(Transaction.IsolationLevel.REPEATABLE_READ)
                .build();

            var result = jq.transaction(tx)
                .bind(new BigDecimal("100.00"), fromId)
                .bind(new BigDecimal("100.00"), toId)
                .execute();

            assertInstanceOf(Ok.class, result);

            var fromTotal = jq.one(
                select(col("total")).from("orders").where(eq(col("id"), param(Long.class))).build(),
                rs -> rs.getBigDecimal("total"), fromId).or(BigDecimal.ZERO);

            var toTotal = jq.one(
                select(col("total")).from("orders").where(eq(col("id"), param(Long.class))).build(),
                rs -> rs.getBigDecimal("total"), toId).or(BigDecimal.ZERO);

            assertEquals(0, new BigDecimal("400.00").compareTo(fromTotal));
            assertEquals(0, new BigDecimal("200.00").compareTo(toTotal));
        }

        @Test
        void wrongBindCount_returnsErr() {
            var tx = transaction()
                .add(updateUserName())
                .add(updateUserActive())
                .build();

            var result = jq.transaction(tx)
                .bind("OnlyOne", 1L)
                .execute();

            assertInstanceOf(Err.class, result);
        }

        @Test
        void bind_isImmutable() {
            var tx = transaction()
                .add(updateUserName())
                .build();

            var bound1 = jq.transaction(tx).bind("Name1", 1L);
            var bound2 = bound1.bind("Name2", 2L);

            assertNotSame(bound1, bound2);
        }

        @Test
        void isolationSerializable_works() {
            var id = nextId();

            var tx = transaction()
                .add(insertUser())
                .isolationLevel(Transaction.IsolationLevel.SERIALIZABLE)
                .build();

            var result = jq.transaction(tx)
                .bind(id, "Nu", "nu@example.com", true)
                .execute();

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void failedTransaction_rollsBack() {
            var id = nextId();
        
            var divByZero = update("users")
                .set("id", divide(col("id"), lit(0)))
                .build();
        
            var tx = transaction()
                .add(insertUser())
                .add(divByZero)
                .build();
        
            var result = jq.transaction(tx)
                .bind(id, "Omicron", "omicron@example.com", true)
                .execute();
        
            assertInstanceOf(Err.class, result);
        
            var count = jq.one(selectUserCount(), rs -> rs.getLong(1)).or(0L);
            assertEquals(0L, count);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void one_noRows_isErrNotException() {
            var result = jq.one(selectUserById(), userRow(), 999999L);

            assertInstanceOf(Err.class, result);
            assertDoesNotThrow(() -> result.errOptional().isPresent());
        }

        @Test
        void option_never_throws() {
            assertDoesNotThrow(() ->
                jq.option(selectUserById(), userRow(), 999999L)
            );
        }

        @Test
        void many_emptyTable_returnsOkEmptyList() {
            var result = jq.many(selectAllUsers(), userRow());

            assertInstanceOf(Ok.class, result);
            assertTrue(result.isOk());
            assertTrue(result instanceof Ok(var value) && value.isEmpty());
        }
    }

    @Nested
    class UnionQueries {

        private static JQ.Read activeUsers() {
            return select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build();
        }

        private static JQ.Read usersByEmail() {
            return select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(String.class)))
                .build();
        }

        @Test
        void twoParameterizedQueries_bindInAddedOrder() {
            insertTestUser("Priya", "priya@example.com", true);
            insertTestUser("Omar", "omar@example.com", false);

            var union = new UnionBuilder(UnionType.UNION, activeUsers(), usersByEmail()).build();

            var result = jq.many(union, idName(), true, "omar@example.com");

            assertInstanceOf(Ok.class, result);
            var rows = result.or(List.of());
            assertEquals(2, rows.size());
            assertTrue(rows.stream().anyMatch(r -> r.name().equals("Priya")));
            assertTrue(rows.stream().anyMatch(r -> r.name().equals("Omar")));
        }

        @Test
        void wrongParamOrder_returnsErrOrEmpty() {
            insertTestUser("Sana", "sana@example.com", true);

            var union = new UnionBuilder(UnionType.UNION, activeUsers(), usersByEmail()).build();

            var result = jq.many(union, rs -> "success", "sana@example.com", true);

            assertTrue(result instanceof Err<?, ?> || result.or(List.of()).isEmpty());
        }

        @Test
        void threeQueries_allParamsBindSequentially() {
            var userId = insertTestUser("Leo", "leo@example.com", true);
            insertTestOrder(userId, new BigDecimal("500.00"));

            var union = new UnionBuilder(UnionType.UNION, activeUsers(), usersByEmail())
                .add(select(col("id"), col("status")).from("orders")
                    .where(eq(col("status"), param(String.class)))
                    .build())
                .build();

            var result = jq.many(union, longId(), true, "nobody@example.com", "pending");

            assertInstanceOf(Ok.class, result);
            assertFalse(result.or(List.of()).isEmpty());
        }

        @Test
        void unionAll_withDuplicateParamsAcrossQueries_bothBind() {
            insertTestUser("Won", "won@example.com", true);

            var union = new UnionBuilder(UnionType.UNION_ALL, activeUsers(), activeUsers()).build();

            var result = jq.many(union, idName(), true, true);

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }

        @Test
        void missingOneParam_returnsErr() {
            var union = new UnionBuilder(UnionType.UNION, activeUsers(), usersByEmail()).build();

            var result = jq.many(union, idName(), true);

            assertInstanceOf(Err.class, result);
        }

        @Test
        void extraParam_returnsErr() {
            insertTestUser("Tao", "tao@example.com", true);

            var union = new UnionBuilder(UnionType.UNION, activeUsers(), usersByEmail()).build();

            var result = jq.many(union, idName(), true, "tao@example.com", "extra");

            assertInstanceOf(Err.class, result);
        }

        @Test
        void unionInsideTransaction_bindsCorrectly() {
            insertTestUser("Rene", "rene@example.com", true);

            var union = new UnionBuilder(UnionType.UNION, activeUsers(), usersByEmail()).build();

            var tx = transaction().add(union).build();

            var result = jq.transaction(tx)
                .bind(true, "nobody@example.com")
                .execute();

            assertInstanceOf(Ok.class, result);
        }
    }

    @Nested
    class CTEQueries {

        private static JQ.Read allUsers() {
            return select(col("id"), col("name")).from("users").build();
        }

        private static JQ.Read activeUsersOnly() {
            return select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build();
        }

        private static JQ.Read usersByEmailOnly() {
            return select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(String.class)))
                .build();
        }

        private static JQ.Read finalSelectById() {
            return select(col("id"), col("name"))
                .from("users")
                .where(eq(col("id"), param(Long.class)))
                .build();
        }

        @Test
        void parameterFreeCte_finalQueryParamsBindCorrectly() {
            var id = insertTestUser("Petra", "petra@example.com");

            var cte = new CTEBuilder("all_users", allUsers())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), id);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(List.of()).size());
        }

        @Test
        void parameterizedWithQuery_bothParamsBindInOrder() {
            insertTestUser("Ravi", "ravi@example.com", true);
            var targetId = insertTestUser("Suki", "suki@example.com", true);

            var cte = new CTEBuilder("active_only", activeUsersOnly())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), true, targetId);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(List.of()).size());
            assertEquals("Suki", result.or(List.of()).get(0).name());
        }

        @Test
        void parameterizedWithQuery_wrongParamCount_returnsErr() {
            insertTestUser("Ito", "ito@example.com", true);

            var cte = new CTEBuilder("active_only", activeUsersOnly())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), true);

            assertInstanceOf(Err.class, result);
        }

        @Test
        void parameterizedWithQuery_paramsInWrongOrder_returnsErrOrEmpty() {
            var id = insertTestUser("Nadia", "nadia@example.com", true);

            var cte = new CTEBuilder("active_only", activeUsersOnly())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), id, true);

            assertTrue(result instanceof Err<?, ?> || result.or(List.of()).isEmpty());
        }

        @Test
        void multipleParameterizedWithEntries_allParamsBindInDeclarationOrder() {
            var id = insertTestUser("Omar", "omar@example.com", true);

            var cte = new CTEBuilder("active_only", activeUsersOnly())
                .with("by_email", usersByEmailOnly())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), true, "nobody@example.com", id);

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void recursiveCte_finalQueryParamsBind() {
            var id = insertTestUser("Noor", "noor@example.com");

            var cte = new CTEBuilder("tree", allUsers())
                .withRecursive("subtree", allUsers(), allUsers())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), id);

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void recursiveCte_baseAndRecursiveParamsBindBeforeFinal() {
            var id = insertTestUser("Kofi", "kofi@example.com", true);

            var cte = new CTEBuilder("seed", allUsers())
                .withRecursive("tree", activeUsersOnly(), usersByEmailOnly())
                .build(finalSelectById());

            var result = jq.many(cte, idName(), true, "nobody@example.com", id);

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void writeFinalQuery_withQueryParams_precedeFinalParams() {
            var id = insertTestUser("Zara", "zara@example.com", true);

            var finalDelete = new DeleteBuilder("users")
                .where(eq(col("id"), param(Long.class)))
                .build();

            var cte = new CTEBuilder("active_only", activeUsersOnly())
                .build(finalDelete);

            var result = jq.write(cte, true, id);

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(0));
        }

        @Test
        void crossReferencingCteEntries_throwsSchemaVerification() {
            assertThrows(Exception.class, () ->
                new CTEBuilder("a", allUsers())
                    .with("b", select(col("id")).from("a").build())
            );
        }

        @Test
        void withQueryBuiltWithOuter_doesNotThrowAtBuildTime() {
            var outer = allUsers();

            var withQuery = select(col("id"), col("name"))
                .from("orders")
                .where(eq(col("user_id"), param(Long.class)))
                .build(outer);

            assertDoesNotThrow(() ->
                new CTEBuilder("orders_cte", withQuery).build(finalSelectById())
            );
        }

        @Test
        void withQueryBuiltWithOuter_paramTypesStillIncludesItsOwnParams() {
            var outer = allUsers();

            var withQuery = select(col("id"), col("name"))
                .from("orders")
                .where(eq(col("user_id"), param(Long.class)))
                .build(outer);

            var cte = new CTEBuilder("orders_cte", withQuery).build(finalSelectById());

            var paramCount = ((Context) cte.context()).paramTypes().size();

            assertEquals(2, paramCount);
        }

        @Test
        void cteWithUnionAsWithQuery_paramsBindInOrder() {
            var id = insertTestUser("Hugo", "hugo@example.com", true);

            var union = new UnionBuilder(UnionType.UNION, activeUsersOnly(), usersByEmailOnly()).build();

            var cte = new CTEBuilder("combined", union)
                .build(finalSelectById());

            var result = jq.many(cte, idName(), true, "nobody@example.com", id);
            assertInstanceOf(Ok.class, result);
        }

        @Test
        void cteInsideTransaction_bindsCorrectly() {
            var id = insertTestUser("Ines", "ines@example.com", true);

            var cte = new CTEBuilder("active_only", activeUsersOnly())
                .build(finalSelectById());

            var tx = transaction().add(cte).build();

            var result = jq.transaction(tx).bind(true, id).execute();

            assertInstanceOf(Ok.class, result);
        }
    }

    @Nested
    class ParameterizedSubqueryExecution {

        @Test
        void inSubqueryWithParam_nowBindsAndExecutesCorrectly() {
            var userId = insertTestUser("Diego", "diego@example.com", true);
            insertTestOrder(userId, new BigDecimal("500.00"));

            var sub = select(col("user_id"))
                .from("orders")
                .where(eq(col("status"), param(String.class)))
                .build();

            var q = select(col("id"), col("name"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                .build();

            var declaredParams = ((Context) q.context()).paramTypes().size();
            assertEquals(1, declaredParams);

            var result = jq.many(q, idName(), "pending");

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void fromSubqueryWithParam_failsWithClearParamCountMessage() {
            insertTestUser("Fatima", "fatima@example.com", true);

            var sub = select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build();

            var q = select(col("id"))
                .from(new Subquery.Table(sub), "active_users")
                .build();

            var result = jq.many(q, longId(), true);

            assertInstanceOf(Err.class, result);
            var message = result.errOptional().orElseThrow().getMessage();
            assertTrue(message.contains("Parameter count mismatch"));
        }

        @Test
        void inSubqueryWithParam_noArgsAtAll_stillErrs() {
            var sub = select(col("user_id"))
                .from("orders")
                .where(eq(col("status"), param(String.class)))
                .build();

            var q = select(col("id"), col("name"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                .build();

            var result = jq.many(q, idName());

            assertInstanceOf(Err.class, result);
        }

        @Test
        void outerParamPlusSubqueryParam_onlyOuterExpected_stillErrsOnExecution() {
            var userId = insertTestUser("Lars", "lars@example.com", true);
            insertTestOrder(userId, new BigDecimal("500.00"));

            var sub = select(col("user_id"))
                .from("orders")
                .where(eq(col("status"), param(String.class)))
                .build();

            var q = select(col("id"), col("name"))
                .from("users")
                .where(and(
                    eq(col("id"), param(Long.class)),
                    new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub)))
                ))
                .build();

            var result = jq.many(q, idName(), userId);

            assertInstanceOf(Err.class, result);
        }

        @Test
        void existsSubqueryWithParam_nowBindsAndExecutesCorrectly() {
            var userId = insertTestUser("Mina", "mina@example.com", true);
            insertTestOrder(userId, new BigDecimal("500.00"));

            var sub = select(col("id"))
                .from("orders")
                .where(eq(col("status"), param(String.class)))
                .build();

            var q = select(col("id"), col("name"))
                .from("users")
                .where(exists(table(sub)))
                .build();

            var declaredParams = ((Context) q.context()).paramTypes().size();
            assertEquals(1, declaredParams);

            var result = jq.many(q, idName(), "pending");

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void ordinaryParameterizedQuery_noFalsePositive() {
            var id = insertTestUser("Owen", "owen@example.com", true);

            var result = jq.one(selectUserById(), userRow(), id);

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void unionQuery_noFalsePositive() {
            insertTestUser("Priya2", "priya2@example.com", true);

            var union = new UnionBuilder(UnionType.UNION,
                select(col("id"), col("name")).from("users")
                    .where(eq(col("active"), param(Boolean.class))).build(),
                select(col("id"), col("name")).from("users")
                    .where(eq(col("email"), param(String.class))).build()
            ).build();

            var result = jq.many(union, idName(), true, "nobody@example.com");

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void cteQuery_noFalsePositive() {
            var id = insertTestUser("Quang", "quang@example.com", true);

            var cte = new CTEBuilder("active_only",
                select(col("id"), col("name")).from("users")
                    .where(eq(col("active"), param(Boolean.class))).build())
                .build(select(col("id"), col("name")).from("users")
                    .where(eq(col("id"), param(Long.class))).build());

            var result = jq.many(cte, idName(), true, id);

            assertInstanceOf(Ok.class, result);
        }

        @Test
        void cteWithOuterBuiltWithQueryAndSubqueryInFinal_allCombined() {
            var userId = insertTestUser("Sami", "sami@example.com", true);
            insertTestOrder(userId, new BigDecimal("500.00"));

            var outer = select(col("id"), col("name")).from("users").build();

            var withQuery = select(col("id"), col("name"))
                .from("orders")
                .where(eq(col("user_id"), param(Long.class)))
                .build(outer);

            var sub = select(col("user_id"))
                .from("orders")
                .where(eq(col("status"), param(String.class)))
                .build();

            var finalQuery = select(col("id"), col("name"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                .build();

            var cte = new CTEBuilder("orders_cte", withQuery).build(finalQuery);

            // withQuery's own Long.class param + finalQuery's embedded subquery String.class param are both counted now.
            var declaredParams = ((Context) cte.context()).paramTypes().size();
            assertEquals(2, declaredParams);

            var result = jq.many(cte, idName(), userId);

            assertInstanceOf(Err.class, result);
            var message = result.errOptional().orElseThrow().getMessage();
            assertTrue(message.contains("Parameter count mismatch"));
        }

        @Test
        void cteWithOuterBuiltWithQueryAndSubqueryInFinal_correctArgCount_executesSuccessfully() {
            var userId = insertTestUser("Nadia", "nadia@example.com", true);
            insertTestOrder(userId, new BigDecimal("500.00"));

            var outer = select(col("id"), col("name")).from("users").build();

            var withQuery = select(col("id"))
                .from("orders")
                .where(eq(col("user_id"), param(Long.class)))
                .build(outer);

            var sub = select(col("user_id"))
                .from("orders")
                .where(eq(col("status"), param(String.class)))
                .build();

            var finalQuery = select(col("id"), col("name"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                .build();

            var cte = new CTEBuilder("orders_cte", withQuery).build(finalQuery);

            assertEquals(2, ((Context) cte.context()).paramTypes().size());

            var result = jq.many(cte, idName(), userId, "pending");

            assertInstanceOf(Ok.class, result);
        }
    }
}