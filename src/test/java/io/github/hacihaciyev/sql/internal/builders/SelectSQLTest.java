package io.github.hacihaciyev.sql.internal.builders;

import io.github.hacihaciyev.sql.SQL;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.internal.value_objects.FromSource;
import io.github.hacihaciyev.sql.internal.value_objects.JoinEntry;
import io.github.hacihaciyev.sql.internal.value_objects.ForUpdate;
import io.github.hacihaciyev.sql.value_objects.*;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static io.github.hacihaciyev.sql.expressions.BinaryOp.BinaryOperator.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class SelectSQLTest {

    private static ColumnRef.Base col(String name) {
        return new ColumnRef.Base(name);
    }

    private static ColumnRef.VariableBase col(String table, String name) {
        return new ColumnRef.VariableBase(table, name);
    }
    
    private static ColumnRef.VariableAlias col(String table, String name, String alias) {
        return new ColumnRef.VariableAlias(table, name, alias);
    }

    private static BinaryOp eq(Expr left, Expr right) {
        return new BinaryOp(EQ, left, right);
    }

    private static BinaryOp gt(Expr left, Expr right) {
        return new BinaryOp(GT, left, right);
    }

    private static BinaryOp and(Expr left, Expr right) {
        return new BinaryOp(AND, left, right);
    }

    private static Literal.IntLiteral lit(int v) {
        return new Literal.IntLiteral(v);
    }

    private static Literal.StringLiteral lit(String v) {
        return new Literal.StringLiteral(v);
    }

    private static FromSource.Physical from(String table) {
        return new FromSource.Physical(new TableRef.Base(table));
    }

    private static FromSource.Physical from(TableRef tref) {
        return new FromSource.Physical(tref);
    }

    @Nested
    class Projections {

        @Test
        void singleColumn() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users", sql);
        }

        @Test
        void multipleColumns() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id")), new Projection.Base(col("name"))),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id, name FROM users", sql);
        }

        @Test
        void aliasedColumn() {
            var sql = SelectSQL.build(
                List.of(new Projection.Aliased(new Func.Count(col("id"), false), "total")),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT COUNT(id) AS total FROM users", sql);
        }

        @Test
        void wildcard() {
            var sql = SelectSQL.build(
                List.of(new Projection.Wildcard()),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT * FROM users", sql);
        }

        @Test
        void qualifiedWildcard() {
            var sql = SelectSQL.build(
                List.of(new Projection.QualifiedWildcard("u")),
                false, from(new TableRef.AliasedBase("users", "u")), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT u.* FROM users AS u", sql);
        }

        @Test
        void distinct() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("status"))),
                true, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT DISTINCT status FROM orders", sql);
        }

        @Test
        void aggregate_count() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(new Func.CountAll())),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT COUNT(*) FROM users", sql);
        }

        @Test
        void aggregate_sum() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(new Func.Sum(col("total"), false))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT SUM(total) FROM orders", sql);
        }

        @Test
        void caseExpression() {
            var branches = List.of(
                new CaseExpr.WhenThen(eq(col("status"), lit("active")), lit("Active"))
            );
            var sql = SelectSQL.build(
                List.of(new Projection.Base(new CaseExpr.CaseElse(branches, lit("Other")))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT CASE WHEN (status = 'active') THEN 'Active' ELSE 'Other' END FROM orders",
                sql
            );
        }
    }

    @Nested
    class From {

        @Test
        void tableWithAlias() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "id"))),
                false, from(new TableRef.AliasedBase("users", "u")), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT u.id FROM users AS u", sql);
        }

        @Test
        void tableWithSchema() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from(new TableRef.WithSchema("public", "users")), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM public.users", sql);
        }
    }

    @Nested
    class Joins {

        @Test
        void innerJoin() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "name")), new Projection.Base(col("o", "total"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.inner(from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT u.name, o.total FROM users AS u JOIN orders AS o ON (u.id = o.user_id)",
                sql
            );
        }

        @Test
        void leftJoin() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "name")), new Projection.Base(col("o", "total"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.left(from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT u.name, o.total FROM users AS u LEFT JOIN orders AS o ON (u.id = o.user_id)",
                sql
            );
        }

        @Test
        void rightJoin() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "name")), new Projection.Base(col("o", "total"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.right(from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT u.name, o.total FROM users AS u RIGHT JOIN orders AS o ON (u.id = o.user_id)",
                sql
            );
        }

        @Test
        void fullJoin() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "name")), new Projection.Base(col("o", "total"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.full(from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT u.name, o.total FROM users AS u FULL JOIN orders AS o ON (u.id = o.user_id)",
                sql
            );
        }

        @Test
        void crossJoin() {
            var sql = SelectSQL.build(
                List.of(
                    new Projection.Base(col("u", "id", "user_id")),
                    new Projection.Base(col("o", "id", "order_id"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.cross(from(new TableRef.AliasedBase("orders", "o")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT u.id AS user_id, o.id AS order_id FROM users AS u CROSS JOIN orders AS o",
                sql
            );
        }

        @Test
        void multipleJoins() {
            var sql = SelectSQL.build(
                List.of(
                    new Projection.Base(col("u", "name")),
                    new Projection.Base(col("o", "total")),
                    new Projection.Base(col("i", "product"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(
                    JoinEntry.inner(from(new TableRef.AliasedBase("orders", "o")),
                        eq(col("u", "id"), col("o", "user_id"))),
                    JoinEntry.inner(from(new TableRef.AliasedBase("order_items", "i")),
                        eq(col("o", "id"), col("i", "order_id")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                """
                SELECT u.name, o.total, i.product FROM users AS u \
                JOIN orders AS o ON (u.id = o.user_id) \
                JOIN order_items AS i ON (o.id = i.order_id)""",
                sql
            );
        }
    }

    @Nested
    class Where {

        @Test
        void simpleEquality() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.of(eq(col("id"), lit(1))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users WHERE (id = 1)", sql);
        }

        @Test
        void compoundCondition() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.of(and(
                    eq(col("active"), new Literal.BooleanLiteral(true)),
                    gt(col("id"), lit(0)))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users WHERE ((active = TRUE) AND (id > 0))", sql);
        }

        @Test
        void isNull() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.of(new IsNullExpr.IsNull(col("email"))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users WHERE email IS NULL", sql);
        }

        @Test
        void inList() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.of(new InExpr.In(col("status"),
                    new InExpr.ValueList(List.of(lit("active"), lit("pending"))))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM orders WHERE status IN ('active', 'pending')", sql);
        }

        @Test
        void between() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.of(new BetweenExpr.Between(col("total"), lit(100), lit(500))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM orders WHERE total BETWEEN 100 AND 500", sql);
        }

        @Test
        void like() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.of(new BinaryOp(LIKE, col("name"), lit("John%"))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users WHERE (name LIKE 'John%')", sql);
        }
    }

    @Nested
    class GroupByHaving {

        @Test
        void groupBy() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("status")), new Projection.Base(new Func.CountAll())),
                false, from("orders"), List.of(),
                Optional.empty(),
                List.of(col("status")),
                Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT status, COUNT(*) FROM orders GROUP BY status", sql);
        }

        @Test
        void groupByMultiple() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("status")), new Projection.Base(col("user_id")), new Projection.Base(new Func.CountAll())),
                false, from("orders"), List.of(),
                Optional.empty(),
                List.of(col("status"), col("user_id")),
                Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT status, user_id, COUNT(*) FROM orders GROUP BY status, user_id", sql);
        }

        @Test
        void having() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("status")), new Projection.Base(new Func.CountAll())),
                false, from("orders"), List.of(),
                Optional.empty(),
                List.of(col("status")),
                Optional.of(gt(new Func.CountAll(), lit(10))),
                List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                "SELECT status, COUNT(*) FROM orders GROUP BY status HAVING (COUNT(*) > 10)",
                sql
            );
        }

        @Test
        void whereAndGroupByAndHaving() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("status")), new Projection.Base(new Func.CountAll())),
                false, from("orders"), List.of(),
                Optional.of(gt(col("total"), lit(0))),
                List.of(col("status")),
                Optional.of(gt(new Func.CountAll(), lit(5))),
                List.of(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals(
                """
                SELECT status, COUNT(*) FROM orders \
                WHERE (total > 0) \
                GROUP BY status \
                HAVING (COUNT(*) > 5)""",
                sql
            );
        }
    }

    @Nested
    class OrderBy {

        @Test
        void singleColumn() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id")), new Projection.Base(col("name"))),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(),
                List.of(col("name")),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id, name FROM users ORDER BY name", sql);
        }

        @Test
        void multipleColumns() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id")), new Projection.Base(col("name")), new Projection.Base(col("age"))),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(),
                List.of(col("age"), col("name")),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id, name, age FROM users ORDER BY age, name", sql);
        }

        @Test
        void orderByExpression() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id")), new Projection.Base(new Func.CountAll())),
                false, from("orders"), List.of(),
                Optional.empty(),
                List.of(col("status")),
                Optional.empty(),
                List.of(new Func.CountAll()),
                Optional.empty(), Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id, COUNT(*) FROM orders GROUP BY status ORDER BY COUNT(*)", sql);
        }
    }

    @Nested
    class LimitOffset {

        @Test
        void limitOnly() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.of(new Limit(10)),
                Optional.empty(),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users LIMIT 10", sql);
        }

        @Test
        void limitAndOffset() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("users"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.of(new Limit(10)),
                Optional.of(new Offset(20)),
                Optional.empty()
            );
            assertEquals("SELECT id FROM users LIMIT 10 OFFSET 20", sql);
        }
    }

    @Nested
    class FullQuery {

        @Test
        void allClauses() {
            var sql = SelectSQL.build(
                List.of(
                    new Projection.Base(col("u", "name")),
                    new Projection.Base(col("o", "status")),
                    new Projection.Base(new Func.CountAll())),
                false,
                from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.inner(
                    from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.of(eq(col("u", "active"), new Literal.BooleanLiteral(true))),
                List.of(col("u", "name"), col("o", "status")),
                Optional.of(gt(new Func.CountAll(), lit(5))),
                List.of(col("u", "name")),
                Optional.of(new Limit(10)),
                Optional.of(new Offset(0)),
                Optional.empty()
            );
            assertEquals(
                """
                SELECT u.name, o.status, COUNT(*) FROM users AS u \
                JOIN orders AS o ON (u.id = o.user_id) \
                WHERE (u.active = TRUE) \
                GROUP BY u.name, o.status \
                HAVING (COUNT(*) > 5) \
                ORDER BY u.name \
                LIMIT 10 OFFSET 0""",
                sql
            );
        }
    }

    @Nested
    class ForUpdateTest {
    
        @Test
        void simpleForUpdate() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id")), new Projection.Base(col("total"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.simple())
            );
            assertEquals("SELECT id, total FROM orders FOR UPDATE", sql);
        }
    
        @Test
        void forUpdateNoWait() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.noWait())
            );
            assertEquals("SELECT id FROM orders FOR UPDATE NOWAIT", sql);
        }
    
        @Test
        void forUpdateSkipLocked() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.skipLocked())
            );
            assertEquals("SELECT id FROM orders FOR UPDATE SKIP LOCKED", sql);
        }
    
        @Test
        void forUpdateOf() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "name")), new Projection.Base(col("o", "total"))),
                false, from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.inner(
                    from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.of(List.of(SQL.t("users"), SQL.t("orders"))))
            );
            assertEquals(
                "SELECT u.name, o.total FROM users AS u JOIN orders AS o ON (u.id = o.user_id) FOR UPDATE OF users, orders",
                sql
            );
        }
    
        @Test
        void forUpdateOfNoWait() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.ofNoWait(List.of(SQL.t("orders"))))
            );
            assertEquals("SELECT id FROM orders FOR UPDATE OF orders NOWAIT", sql);
        }
    
        @Test
        void forUpdateOfSkipLocked() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.ofSkipLocked(List.of(SQL.t("orders"))))
            );
            assertEquals("SELECT id FROM orders FOR UPDATE OF orders SKIP LOCKED", sql);
        }
    
        @Test
        void forUpdateWithWhere() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.of(eq(col("status"), lit("pending"))),
                List.of(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.simple())
            );
            assertEquals("SELECT id FROM orders WHERE (status = 'pending') FOR UPDATE", sql);
        }
    
        @Test
        void forUpdateWithOrderBy() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(),
                List.of(col("created_at")),
                Optional.empty(), Optional.empty(),
                Optional.of(ForUpdate.simple())
            );
            assertEquals("SELECT id FROM orders ORDER BY created_at FOR UPDATE", sql);
        }
    
        @Test
        void forUpdateWithLimit() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.of(new Limit(10)),
                Optional.empty(),
                Optional.of(ForUpdate.simple())
            );
            assertEquals("SELECT id FROM orders LIMIT 10 FOR UPDATE", sql);
        }
    
        @Test
        void forUpdateWithLimitAndOffset() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("id"))),
                false, from("orders"), List.of(),
                Optional.empty(), List.of(), Optional.empty(), List.of(),
                Optional.of(new Limit(10)),
                Optional.of(new Offset(20)),
                Optional.of(ForUpdate.noWait())
            );
            assertEquals("SELECT id FROM orders LIMIT 10 OFFSET 20 FOR UPDATE NOWAIT", sql);
        }
    
        @Test
        void forUpdateWithAllClauses() {
            var sql = SelectSQL.build(
                List.of(new Projection.Base(col("u", "name")), new Projection.Base(col("o", "total"))),
                false,
                from(new TableRef.AliasedBase("users", "u")),
                List.of(JoinEntry.inner(
                    from(new TableRef.AliasedBase("orders", "o")),
                    eq(col("u", "id"), col("o", "user_id")))),
                Optional.of(eq(col("u", "active"), new Literal.BooleanLiteral(true))),
                List.of(col("u", "name")),
                Optional.of(gt(new Func.CountAll(), lit(5))),
                List.of(col("u", "name")),
                Optional.of(new Limit(10)),
                Optional.of(new Offset(0)),
                Optional.of(ForUpdate.skipLocked())
            );
            assertEquals(
                """
                SELECT u.name, o.total FROM users AS u \
                JOIN orders AS o ON (u.id = o.user_id) \
                WHERE (u.active = TRUE) \
                GROUP BY u.name \
                HAVING (COUNT(*) > 5) \
                ORDER BY u.name \
                LIMIT 10 OFFSET 0 FOR UPDATE SKIP LOCKED""",
                sql
            );
        }
    }
}