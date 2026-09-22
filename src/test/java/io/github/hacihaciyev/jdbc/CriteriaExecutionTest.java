package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.QueryForge;
import io.github.hacihaciyev.sql.Criteria;

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

import static io.github.hacihaciyev.sql.QueryForge.deleteFrom;
import static io.github.hacihaciyev.sql.QueryForge.insertInto;
import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class CriteriaExecutionTest {

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    private JetQuerious jq;

    @BeforeEach
    void setUp() {
        jq = JetQuerious.defaultInstance();
        jq.write(deleteFrom("users").build());
        jq.write(deleteFrom("employees").build());
    }

    private static long nextId() {
        return ID_SEQ.getAndIncrement();
    }

    private long insertUser(String name, String email, int age, boolean active) {
        var id = nextId();
        jq.write(
            insertInto("users")
                .columns("id", Long.class, "name", String.class, "email", String.class,
                         "age", Integer.class, "active", Boolean.class)
                .build(),
            id, name, email, age, active
        );
        return id;
    }

    private long insertEmployee(String department, boolean active) {
        var id = nextId();
        jq.write(
            insertInto("employees")
                .columns("id", Long.class, "department", String.class, "salary", BigDecimal.class, "active", Boolean.class)
                .build(),
            id, department, new BigDecimal("1000.00"), active
        );
        return id;
    }

    record UserRow(long id, String name, String email) {}

    private static ResultSetExtractor<UserRow> userRow() {
        return rs -> new UserRow(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
    }

    private static ResultSetExtractor<String> name() {
        return rs -> rs.getString("name");
    }

    private static ResultSetExtractor<Long> count() {
        return rs -> rs.getLong("count");
    }

    private static Criteria byEmailAndAge() {
        return QueryForge.criteria(col("id"), col("name"), col("email"))
            .from("users")
            .where(and(
                eq(col("email"), opt(String.class)),
                eq(col("age"), opt(Integer.class))
            ))
            .build();
    }

    private static Criteria byActiveAndOptionalEmail() {
        return QueryForge.criteria(col("id"), col("name"), col("email"))
            .from("users")
            .where(and(
                eq(col("active"), param(Boolean.class)),
                eq(col("email"), opt(String.class))
            ))
            .build();
    }

    @Nested
    class PreviouslyAmbiguousNullArguments {

        @Test
        void leadingNullArg_isTreatedAsAnArgumentNotAResultSetType() {
            insertUser("Alice", "alice@example.com", 30, true);
            insertUser("Bob", "bob@example.com", 40, true);

            var result = jq.criteria(byEmailAndAge(), userRow()).args(null, 40).many();

            assertInstanceOf(Ok.class, result);
            var rows = result.or(List.of());
            assertEquals(1, rows.size());
            assertEquals("Bob", rows.get(0).name());
        }

        @Test
        void allNullArgs_dropWhereEntirely() {
            insertUser("Eve", "eve@example.com", 22, true);
            insertUser("Frank", "frank@example.com", 33, true);

            var result = jq.criteria(byEmailAndAge(), userRow()).args(null, null).many();

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }

        @Test
        void leadingNullArg_withOption() {
            var result = jq.criteria(byEmailAndAge(), userRow()).args(null, 999).option();

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(Optional.empty()).isEmpty());
        }
    }

    @Nested
    class WherePruning {

        @Test
        void nonNullOptionalArg_appliesPredicate() {
            insertUser("Carol", "carol@example.com", 25, true);
            insertUser("Dave", "dave@example.com", 25, true);

            var result = jq.criteria(byEmailAndAge(), userRow()).args("dave@example.com", 25).many();

            assertInstanceOf(Ok.class, result);
            assertEquals("Dave", result.or(List.of()).get(0).name());
        }

        @Test
        void requiredPredicateSurvivesOptionalSiblingBeingNull() {
            insertUser("Gina", "gina@example.com", 28, true);
            insertUser("Hank", "hank@example.com", 28, false);

            var result = jq.criteria(byActiveAndOptionalEmail(), userRow()).args(true, null).many();

            assertInstanceOf(Ok.class, result);
            var rows = result.or(List.of());
            assertEquals(1, rows.size());
            assertEquals("Gina", rows.get(0).name());
        }

        @Test
        void requiredAndOptionalBothApplied() {
            insertUser("Ivan", "ivan@example.com", 45, true);
            insertUser("Jill", "jill@example.com", 45, true);

            var result = jq.criteria(byActiveAndOptionalEmail(), userRow()).args(true, "jill@example.com").many();

            assertInstanceOf(Ok.class, result);
            assertEquals("Jill", result.or(List.of()).get(0).name());
        }
    }

    @Nested
    class HavingPruning {

        @Test
        void nullOptionalHavingArg_dropsHaving() {
            insertUser("Nina", "nina@example.com", 20, true);
            insertUser("Oscar", "oscar@example.com", 21, true);

            var query = QueryForge.criteria(col("active"), countAll())
                .from("users")
                .groupBy(col("active"))
                .having(gt(countAll(), opt(Long.class)))
                .build();

            var result = jq.criteria(query, count()).args((Object) null).many();

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(List.of()).size());
            assertEquals(2L, result.or(List.of()).get(0));
        }

        @Test
        void nonNullOptionalHavingArg_filtersGroups() {
            insertUser("Pam", "pam@example.com", 20, true);
            insertUser("Quinn", "quinn@example.com", 21, false);

            var query = QueryForge.criteria(col("active"), countAll())
                .from("users")
                .groupBy(col("active"))
                .having(gt(countAll(), opt(Long.class)))
                .build();

            var result = jq.criteria(query, count()).args(1L).many();

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(List.of()).isEmpty());
        }
    }

    @Nested
    class GroupByAndOrderByPruning {

        @Test
        void nullOptInGroupByItem_dropsGroupingEntirely() {
            insertEmployee("engineering", true);
            insertEmployee("sales", true);

            var query = QueryForge.criteria(countAll())
                .from("employees")
                .groupBy(coalesce(col("department"), opt(String.class)))
                .build();

            var result = jq.criteria(query, count()).args((Object) null).many();

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(List.of()).size());
            assertEquals(2L, result.or(List.of()).get(0));
        }

        @Test
        void nonNullOptInGroupByItem_keepsGroupingAndBindsValue() {
            insertEmployee("engineering", true);
            insertEmployee(null, true);

            var query = QueryForge.criteria(countAll())
                .from("employees")
                .groupBy(coalesce(col("department"), opt(String.class)))
                .build();

            var result = jq.criteria(query, count()).args("unassigned").many();

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }

        @Test
        void nullOptInOrderByItem_dropsOrdering() {
            insertUser("Zoe", "zoe@example.com", 24, true);
            insertUser("Adam", "adam@example.com", 25, true);

            var query = QueryForge.criteria(col("name"))
                .from("users")
                .orderBy(coalesce(col("name"), opt(String.class)))
                .build();

            var result = jq.criteria(query, name()).args((Object) null).many();

            assertInstanceOf(Ok.class, result);
            assertEquals(2, result.or(List.of()).size());
        }

        @Test
        void nonNullOptInOrderByItem_ordersRows() {
            insertUser("Zoe", "zoe@example.com", 24, true);
            insertUser("Adam", "adam@example.com", 25, true);

            var query = QueryForge.criteria(col("name"))
                .from("users")
                .orderBy(coalesce(col("name"), opt(String.class)))
                .build();

            var result = jq.criteria(query, name()).args("zzz").many();

            assertInstanceOf(Ok.class, result);
            assertEquals(List.of("Adam", "Zoe"), result.or(List.of()));
        }

        @Test
        void everyClausePrunedSimultaneously_returnsPlainSelect() {
            insertUser("Rita", "rita@example.com", 26, true);

            var query = QueryForge.criteria(col("name"))
                .from("users")
                .where(eq(col("email"), opt(String.class)))
                .orderBy(coalesce(col("name"), opt(String.class)))
                .build();

            var result = jq.criteria(query, name()).args(null, null).many();

            assertInstanceOf(Ok.class, result);
            assertEquals(List.of("Rita"), result.or(List.of()));
        }
    }

    @Nested
    class OneAndOption {

        @Test
        void one_matchFound() {
            insertUser("Kara", "kara@example.com", 31, true);

            var result = jq.criteria(byEmailAndAge(), userRow()).args("kara@example.com", null).one();

            assertInstanceOf(Ok.class, result);
            assertEquals("Kara", result.or(() -> { throw new RuntimeException("unexpected"); }).name());
        }

        @Test
        void one_noMatch_returnsErr() {
            var result = jq.criteria(byEmailAndAge(), userRow()).args("ghost@example.com", null).one();
            assertInstanceOf(Err.class, result);
        }

        @Test
        void option_matchFound() {
            insertUser("Liam", "liam@example.com", 27, true);

            var result = jq.criteria(byEmailAndAge(), userRow()).args("liam@example.com", null).option();

            assertInstanceOf(Ok.class, result);
            assertEquals("Liam", result.or(Optional.empty()).orElseThrow().name());
        }

        @Test
        void many_noMatch_returnsEmptyList() {
            var result = jq.criteria(byEmailAndAge(), userRow()).args("nobody@example.com", null).many();

            assertInstanceOf(Ok.class, result);
            assertTrue(result.or(List.of()).isEmpty());
        }
    }

    @Nested
    class UnexpectedInput {

        @Test
        void tooFewArgs_returnsErr() {
            var result = jq.criteria(byEmailAndAge(), userRow()).args((Object) null).many();
            assertInstanceOf(Err.class, result);
        }

        @Test
        void tooManyArgs_returnsErr() {
            var result = jq.criteria(byEmailAndAge(), userRow()).args(null, 1, "extra").many();
            assertInstanceOf(Err.class, result);
        }

        @Test
        void noArgsCallOnParameterizedQuery_returnsErr() {
            var result = jq.criteria(byEmailAndAge(), userRow()).many();
            assertInstanceOf(Err.class, result);
        }

        @Test
        void argsOnParameterlessQuery_returnsErr() {
            var query = QueryForge.criteria(col("name")).from("users").build();

            var result = jq.criteria(query, name()).args("unexpected").many();
            assertInstanceOf(Err.class, result);
        }

        @Test
        void parameterlessQuery_withoutArgsCall_works() {
            insertUser("Sam", "sam@example.com", 35, true);

            var query = QueryForge.criteria(col("name")).from("users").build();

            var result = jq.criteria(query, name()).many();
            assertInstanceOf(Ok.class, result);
            assertEquals(List.of("Sam"), result.or(List.of()));
        }

        @Test
        void wrongArgTypeAtRuntime_returnsErr() {
            var query = QueryForge.criteria(col("name"))
                .from("users")
                .where(eq(col("age"), opt(Integer.class)))
                .build();

            var result = jq.criteria(query, name()).args("not-an-int").many();
            assertInstanceOf(Err.class, result);
        }

        @Test
        void nullQuery_throws() {
            assertThrows(NullPointerException.class, () -> jq.criteria(null, name()));
        }

        @Test
        void nullExtractor_throws() {
            assertThrows(NullPointerException.class, () -> jq.criteria(byEmailAndAge(), null));
        }

        @Test
        void nullArgsArray_throws() {
            assertThrows(NullPointerException.class, () ->
                jq.criteria(byEmailAndAge(), userRow()).args((Object[]) null)
            );
        }

        @Test
        void nullResultSetType_throws() {
            assertThrows(NullPointerException.class, () ->
                jq.criteria(byEmailAndAge(), userRow()).resultSetType(null)
            );
        }
    }

    @Nested
    class ExecutionBuilderSemantics {

        @Test
        void explicitResultSetType_worksWithNullLeadingArg() {
            insertUser("Mona", "mona@example.com", 29, true);

            var result = jq.criteria(byEmailAndAge(), userRow())
                .resultSetType(ResultSetType.SCROLL_INSENSITIVE_READ_ONLY)
                .args(null, 29)
                .many();

            assertInstanceOf(Ok.class, result);
            assertEquals(1, result.or(List.of()).size());
        }

        @Test
        void resultSetTypeOrderRelativeToArgs_doesNotMatter() {
            insertUser("Nate", "nate@example.com", 41, true);

            var before = jq.criteria(byEmailAndAge(), userRow())
                .resultSetType(ResultSetType.SCROLL_INSENSITIVE_READ_ONLY)
                .args(null, 41)
                .many();

            var after = jq.criteria(byEmailAndAge(), userRow())
                .args(null, 41)
                .resultSetType(ResultSetType.SCROLL_INSENSITIVE_READ_ONLY)
                .many();

            assertEquals(before.or(List.of()).size(), after.or(List.of()).size());
        }

        @Test
        void executionStagesAreImmutable_argsDoNotLeakBetweenBranches() {
            insertUser("Olive", "olive@example.com", 50, true);
            insertUser("Peter", "peter@example.com", 51, true);

            var base = jq.criteria(byEmailAndAge(), userRow());

            var byAge50 = base.args(null, 50).many();
            var byAge51 = base.args(null, 51).many();

            assertEquals("Olive", byAge50.or(List.of()).get(0).name());
            assertEquals("Peter", byAge51.or(List.of()).get(0).name());
        }

        @Test
        void argsArrayIsCopiedDefensively() {
            insertUser("Rose", "rose@example.com", 60, true);

            var args = new Object[]{null, 60};
            var execution = jq.criteria(byEmailAndAge(), userRow()).args(args);
            args[1] = 999;

            var result = execution.many();

            assertInstanceOf(Ok.class, result);
            assertEquals("Rose", result.or(List.of()).get(0).name());
        }

        @Test
        void sameQueryReusedAcrossExecutions_isNotMutatedByPruning() {
            insertUser("Tina", "tina@example.com", 70, true);

            var query = byEmailAndAge();

            var pruned = jq.criteria(query, userRow()).args(null, 70).many();
            var full   = jq.criteria(query, userRow()).args("tina@example.com", 70).many();

            assertEquals(1, pruned.or(List.of()).size());
            assertEquals(1, full.or(List.of()).size());
            assertEquals(2, query.context().paramTypesJava().size());
        }
    }
}