package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;

import static io.github.hacihaciyev.sql.expressions.BinaryOp.BinaryOperator.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class SelectBuilderTest {

    private static ColumnRef.Base col(String name) {
        return new ColumnRef.Base(name);
    }

    private static ColumnRef.VariableBase col(String table, String name) {
        return new ColumnRef.VariableBase(table, name);
    }

    private static ColumnRef.Alias colAs(String name, String alias) {
        return new ColumnRef.Alias(name, alias);
    }
    
    private static ColumnRef.VariableAlias colAs(String table, String name, String alias) {
        return new ColumnRef.VariableAlias(table, name, alias);
    }

    private static BinaryOp eq(Expr left, Expr right) {
        return new BinaryOp(EQ, left, right);
    }

    private static BinaryOp and(Expr left, Expr right) {
        return new BinaryOp(AND, left, right);
    }

    private static BinaryOp gt(Expr left, Expr right) {
        return new BinaryOp(GT, left, right);
    }

    private static Literal.IntLiteral lit(int v) {
        return new Literal.IntLiteral(v);
    }

    private static Literal.StringLiteral lit(String v) {
        return new Literal.StringLiteral(v);
    }

    @Nested
    class Projections {

        @Test
        void singleColumn() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .build();

            assertEquals("SELECT id FROM users", q.sql());
        }

        @Test
        void multipleColumns() {
            var q = SelectBuilder.select(col("id"), col("name"), col("email"))
                    .from("users")
                    .build();

            assertEquals("SELECT id, name, email FROM users", q.sql());
        }

        @Test
        void columnWithAlias() {
            var q = SelectBuilder.select(colAs("name", "user_name"))
                    .from("users")
                    .build();

            assertEquals("SELECT name AS user_name FROM users", q.sql());
        }

        @Test
        void qualifiedColumn() {
            var q = SelectBuilder.select(col("u", "id"), col("u", "name"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();

            assertEquals("SELECT u.id, u.name FROM users AS u", q.sql());
        }

        @Test
        void qualifiedColumnWithAlias() {
            var q = SelectBuilder.select(new ColumnRef.VariableAlias("u", "name", "user_name"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();

            assertEquals("SELECT u.name AS user_name FROM users AS u", q.sql());
        }

        @Test
        void wildcardAll() {
            var q = SelectBuilder.selectAll()
                    .from("users")
                    .build();

            assertEquals("SELECT * FROM users", q.sql());
        }

        @Test
        void qualifiedWildcard() {
            var q = SelectBuilder.selectAll("u")
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();

            assertEquals("SELECT u.* FROM users AS u", q.sql());
        }

        @Test
        void distinct() {
            var q = SelectBuilder.selectDistinct(col("status"))
                    .from("orders")
                    .build();

            assertEquals("SELECT DISTINCT status FROM orders", q.sql());
        }

        @Test
        void distinctAll() {
            var q = SelectBuilder.selectAllDistinct()
                    .from("users")
                    .build();

            assertEquals("SELECT DISTINCT * FROM users", q.sql());
        }

        @Test
        void aggregateFunction() {
            var q = SelectBuilder.select(new Func.Count(col("id"), false))
                    .from("users")
                    .build();

            assertEquals("SELECT COUNT(id) FROM users", q.sql());
        }

        @Test
        void aggregateFunctionDistinct() {
            var q = SelectBuilder.select(new Func.Count(col("id"), true))
                    .from("users")
                    .build();

            assertEquals("SELECT COUNT(DISTINCT id) FROM users", q.sql());
        }

        @Test
        void countStar() {
            var q = SelectBuilder.select(new Func.CountAll())
                    .from("users")
                    .build();

            assertEquals("SELECT COUNT(*) FROM users", q.sql());
        }

        @Test
        void multipleAggregates() {
            var q = SelectBuilder.select(
                            new Func.CountAll(),
                            new Func.Sum(col("amount"), false),
                            new Func.Avg(col("amount"), false)
                    )
                    .from("orders")
                    .build();

            assertEquals("SELECT COUNT(*), SUM(amount), AVG(amount) FROM orders", q.sql());
        }

        @Test
        void scalarSubqueryAsProjection() {
            var sub = SelectBuilder.select(new Func.CountAll())
                    .from("orders")
                    .build();

            var q = SelectBuilder.select(col("name"), new Subquery.Scalar(sub))
                    .from("users")
                    .build();

            assertEquals("SELECT name, (SELECT COUNT(*) FROM orders) FROM users", q.sql());
        }

        @Test
        void caseExpression() {
            var branches = List.of(
                    new CaseExpr.WhenThen(eq(col("status"), lit("active")), lit("Active")),
                    new CaseExpr.WhenThen(eq(col("status"), lit("inactive")), lit("Inactive"))
            );
            var q = SelectBuilder.select(new CaseExpr.CaseElse(branches, lit("Unknown")))
                    .from("orders")
                    .build();

            assertEquals(
                    """
                    SELECT CASE WHEN (status = 'active') THEN 'Active' \
                    WHEN (status = 'inactive') THEN 'Inactive' \
                    ELSE 'Unknown' END FROM orders""",
                    q.sql()
            );
        }
    }

    @Nested
    class From {

        @Test
        void simpleTableName() {
            var q = SelectBuilder.select(col("id")).from("users").build();
            assertEquals("SELECT id FROM users", q.sql());
        }

        @Test
        void tableRef() {
            var q = SelectBuilder.select(col("id"))
                    .from(new TableRef.Base("users"))
                    .build();
            assertEquals("SELECT id FROM users", q.sql());
        }

        @Test
        void tableWithSchema() {
            var q = SelectBuilder.select(col("id"))
                    .from(new TableRef.WithSchema("public", "users"))
                    .build();
            assertEquals("SELECT id FROM public.users", q.sql());
        }

        @Test
        void tableWithCatalogAndSchema() {
            var q = SelectBuilder.select(col("id"))
                    .from(new TableRef.WithCatalogAndSchema("testdb", "public", "users"))
                    .build();
            assertEquals("SELECT id FROM testdb.public.users", q.sql());
        }

        @Test
        void tableWithAlias() {
            var q = SelectBuilder.select(col("u", "id"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();
            assertEquals("SELECT u.id FROM users AS u", q.sql());
        }

        @Test
        void subqueryAsFrom() {
            var sub = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .where(eq(col("active"), new Literal.BooleanLiteral(true)))
                    .build();

            var q = SelectBuilder.select(col("id"))
                    .from(new Subquery.Table(sub), "active_users")
                    .build();

            assertEquals(
                    """
                    SELECT id FROM \
                    (SELECT id, name FROM users WHERE (active = TRUE)) AS active_users""",
                    q.sql()
            );
        }
    }

    @Nested
    class Where {

        @Test
        void simpleEquality() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .where(eq(col("id"), lit(1)))
                    .build();

            assertEquals("SELECT id FROM users WHERE (id = 1)", q.sql());
        }

        @Test
        void compoundCondition() {
            var q = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .where(and(eq(col("active"), new Literal.BooleanLiteral(true)), gt(col("age"), lit(18))))
                    .build();

            assertEquals("SELECT id, name FROM users WHERE ((active = TRUE) AND (age > 18))", q.sql());
        }

        @Test
        void isNull() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .where(new IsNullExpr.IsNull(col("email")))
                    .build();

            assertEquals("SELECT id FROM users WHERE email IS NULL", q.sql());
        }

        @Test
        void isNotNull() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .where(new IsNullExpr.IsNotNull(col("email")))
                    .build();

            assertEquals("SELECT id FROM users WHERE email IS NOT NULL", q.sql());
        }

        @Test
        void inList() {
            var q = SelectBuilder.select(col("id"))
                    .from("orders")
                    .where(new InExpr.In(col("status"), new InExpr.ValueList(List.of(lit("active"), lit("pending")))))
                    .build();

            assertEquals("SELECT id FROM orders WHERE status IN ('active', 'pending')", q.sql());
        }

        @Test
        void notInList() {
            var q = SelectBuilder.select(col("id"))
                    .from("orders")
                    .where(new InExpr.NotIn(col("status"), new InExpr.ValueList(List.of(lit("banned"), lit("deleted")))))
                    .build();

            assertEquals("SELECT id FROM orders WHERE status NOT IN ('banned', 'deleted')", q.sql());
        }

        @Test
        void between() {
            var q = SelectBuilder.select(col("id"))
                    .from("orders")
                    .where(new BetweenExpr.Between(col("amount"), lit(100), lit(500)))
                    .build();

            assertEquals("SELECT id FROM orders WHERE amount BETWEEN 100 AND 500", q.sql());
        }

        @Test
        void like() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .where(new BinaryOp(LIKE, col("name"), lit("John%")))
                    .build();

            assertEquals("SELECT id FROM users WHERE (name LIKE 'John%')", q.sql());
        }

        @Test
        void subqueryInWhere() {
            var sub = SelectBuilder.select(col("user_id"))
                    .from("orders")
                    .where(gt(col("amount"), lit(1000)))
                    .build();

            var q = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                    .build();

            assertEquals(
                    """
                    SELECT id, name FROM users \
                    WHERE id IN (SELECT user_id FROM orders WHERE (amount > 1000))""",
                    q.sql()
            );
        }

        @Test
        void existsSubquery() {
            var outer = SelectBuilder.select(col("u", "id"), col("u", "name"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();
        
            var sub = SelectBuilder.select(col("u", "id"))
                    .from(new TableRef.AliasedBase("orders", "o"))
                    .where(eq(col("o", "user_id"), col("u", "id")))
                    .build(outer);
        
            var q = SelectBuilder.select(col("u", "id"), col("u", "name"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .where(new Exists(new Subquery.Table(sub)))
                    .build();
        
            assertEquals(
                    "SELECT u.id, u.name FROM users AS u " +
                    "WHERE EXISTS (SELECT u.id FROM orders AS o WHERE (o.user_id = u.id))",
                    q.sql()
                    );
        }
    }

    @Nested
    class Joins {

        @Test
        void innerJoin() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "total"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .join(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .build();

            assertEquals(
                    "SELECT u.name, o.total FROM users AS u JOIN orders AS o ON (u.id = o.user_id)",
                    q.sql()
            );
        }

        @Test
        void leftJoin() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "total"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .leftJoin(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .build();

            assertEquals(
                    "SELECT u.name, o.total FROM users AS u LEFT JOIN orders AS o ON (u.id = o.user_id)",
                    q.sql()
            );
        }

        @Test
        void rightJoin() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "total"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .rightJoin(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .build();

            assertEquals(
                    "SELECT u.name, o.total FROM users AS u RIGHT JOIN orders AS o ON (u.id = o.user_id)",
                    q.sql()
            );
        }

        @Test
        void fullJoin() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "total"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .fullJoin(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .build();

            assertEquals(
                    "SELECT u.name, o.total FROM users AS u FULL JOIN orders AS o ON (u.id = o.user_id)",
                    q.sql()
            );
        }

        @Test
        void crossJoin() {
            var q = SelectBuilder.select(colAs("u", "id", "user_id"), colAs("o", "id", "order_id"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .crossJoin(new TableRef.AliasedBase("orders", "o"))
                    .build();
                    
            assertEquals(
                    "SELECT u.id AS user_id, o.id AS order_id FROM users AS u CROSS JOIN orders AS o",
                    q.sql()
            );
        }

        @Test
        void multipleJoins() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "total"), col("i", "product"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .join(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .join(new TableRef.AliasedBase("order_items", "i"), eq(col("o", "id"), col("i", "order_id")))
                    .build();

            assertEquals(
                    """
                    SELECT u.name, o.total, i.product FROM users AS u \
                    JOIN orders AS o ON (u.id = o.user_id) \
                    JOIN order_items AS i ON (o.id = i.order_id)""",
                    q.sql()
            );
        }

        @Test
        void joinOnSubquery() {
            var sub = SelectBuilder.select(col("user_id"), new Func.Sum(col("total"), false))
                    .from("orders")
                    .groupBy(col("user_id"))
                    .build();

            var q = SelectBuilder.select(col("u", "name"), col("o", "user_id"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .join(new Subquery.Table(sub), "o", eq(col("u", "id"), col("o", "user_id")))
                    .build();

            assertEquals(
                    """
                    SELECT u.name, o.user_id FROM users AS u \
                    JOIN (SELECT user_id, SUM(total) FROM orders GROUP BY user_id) AS o \
                    ON (u.id = o.user_id)""",
                    q.sql()
            );
        }

        @Test
        void joinWithWhereAndOrderBy() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "total"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .join(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .where(gt(col("o", "total"), lit(100)))
                    .orderBy(col("o", "total"))
                    .build();

            assertEquals(
                    """
                    SELECT u.name, o.total FROM users AS u \
                    JOIN orders AS o ON (u.id = o.user_id) \
                    WHERE (o.total > 100) \
                    ORDER BY o.total""",
                    q.sql()
            );
        }
    }

    @Nested
    class GroupByHaving {

        @Test
        void simpleGroupBy() {
            var q = SelectBuilder.select(col("status"), new Func.CountAll())
                    .from("orders")
                    .groupBy(col("status"))
                    .build();

            assertEquals("SELECT status, COUNT(*) FROM orders GROUP BY status", q.sql());
        }

        @Test
        void groupByMultipleColumns() {
            var q = SelectBuilder.select(col("status"), col("user_id"), new Func.CountAll())
                    .from("orders")
                    .groupBy(col("status"), col("user_id"))
                    .build();

            assertEquals("SELECT status, user_id, COUNT(*) FROM orders GROUP BY status, user_id", q.sql());
        }

        @Test
        void groupByWithHaving() {
            var q = SelectBuilder.select(col("status"), new Func.CountAll())
                    .from("orders")
                    .groupBy(col("status"))
                    .having(gt(new Func.CountAll(), lit(10)))
                    .build();

            assertEquals(
                    "SELECT status, COUNT(*) FROM orders GROUP BY status HAVING (COUNT(*) > 10)",
                    q.sql()
            );
        }

        @Test
        void groupByWithWhereAndHaving() {
            var q = SelectBuilder.select(col("department"), new Func.Avg(col("salary"), false))
                    .from("employees")
                    .where(eq(col("active"), new Literal.BooleanLiteral(true)))
                    .groupBy(col("department"))
                    .having(gt(new Func.Avg(col("salary"), false), lit(50000)))
                    .build();

            assertEquals(
                    """
                    SELECT department, AVG(salary) FROM employees \
                    WHERE (active = TRUE) \
                    GROUP BY department \
                    HAVING (AVG(salary) > 50000)""",
                    q.sql()
            );
        }
    }

    @Nested
    class OrderBy {

        @Test
        void singleColumn() {
            var q = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .orderBy(col("name"))
                    .build();

            assertEquals("SELECT id, name FROM users ORDER BY name", q.sql());
        }

        @Test
        void multipleColumns() {
            var q = SelectBuilder.select(col("id"), col("name"), col("age"))
                    .from("users")
                    .orderBy(col("age"), col("name"))
                    .build();

            assertEquals("SELECT id, name, age FROM users ORDER BY age, name", q.sql());
        }

        @Test
        void orderByStringHelper() {
            var q = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .orderBy("name", "id")
                    .build();

            assertEquals("SELECT id, name FROM users ORDER BY name, id", q.sql());
        }
    }

    @Nested
    class LimitOffset {

        @Test
        void limitOnly() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .limit(10)
                    .build();

            assertEquals("SELECT id FROM users LIMIT 10", q.sql());
        }

        @Test
        void limitAndOffset() {
            var q = SelectBuilder.select(col("id"))
                    .from("users")
                    .limit(10)
                    .offset(20);

            assertEquals("SELECT id FROM users LIMIT 10 OFFSET 20", q.sql());
        }

        @Test
        void fullPagination() {
            var q = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .where(eq(col("active"), new Literal.BooleanLiteral(true)))
                    .orderBy(col("name"))
                    .limit(25)
                    .offset(50);

            assertEquals(
                    "SELECT id, name FROM users WHERE (active = TRUE) ORDER BY name LIMIT 25 OFFSET 50",
                    q.sql()
            );
        }
    }

    @Nested
    class FullQueries {

        @Test
        void fullSelectWithAllClauses() {
            var q = SelectBuilder.select(col("u", "name"), col("o", "status"), new Func.CountAll())
                    .from(new TableRef.AliasedBase("users", "u"))
                    .join(new TableRef.AliasedBase("orders", "o"), eq(col("u", "id"), col("o", "user_id")))
                    .where(eq(col("u", "active"), new Literal.BooleanLiteral(true)))
                    .groupBy(col("u", "name"), col("o", "status"))
                    .having(gt(new Func.CountAll(), lit(5)))
                    .orderBy(col("u", "name"))
                    .limit(10)
                    .offset(0);

            assertEquals(
                    """
                    SELECT u.name, o.status, COUNT(*) FROM users AS u \
                    JOIN orders AS o ON (u.id = o.user_id) \
                    WHERE (u.active = TRUE) \
                    GROUP BY u.name, o.status \
                    HAVING (COUNT(*) > 5) \
                    ORDER BY u.name \
                    LIMIT 10 OFFSET 0""",
                    q.sql()
            );
        }

        @Test
        void nestedSubqueries() {
            var innerSub = SelectBuilder.select(col("user_id"))
                    .from("banned_users")
                    .build();

            var outerSub = SelectBuilder.select(col("id"), col("name"))
                    .from("users")
                    .where(new InExpr.NotIn(col("id"), new InExpr.SubquerySource(new Subquery.Table(innerSub))))
                    .build();

            var q = SelectBuilder.select(col("id"))
                    .from(new Subquery.Table(outerSub), "active")
                    .where(eq(col("name"), lit("John")))
                    .build();

            assertEquals(
                    """
                    SELECT id FROM \
                    (SELECT id, name FROM users \
                    WHERE id NOT IN (SELECT user_id FROM banned_users)) AS active \
                    WHERE (name = 'John')""",
                    q.sql()
            );
        }

        @Test
        void subqueryAsFromWithJoin() {
            var topBuyers = SelectBuilder.select(col("user_id"), new Func.Sum(col("total"), false))
                    .from("orders")
                    .groupBy(col("user_id"))
                    .having(gt(new Func.Sum(col("total"), false), lit(10000)))
                    .build();

            var q = SelectBuilder.select(col("u", "name"), col("t", "user_id"))
                    .from(new Subquery.Table(topBuyers), "t")
                    .join(new TableRef.AliasedBase("users", "u"), eq(col("t", "user_id"), col("u", "id")))
                    .orderBy(col("t", "user_id"))
                    .build();

            assertEquals(
                    """
                    SELECT u.name, t.user_id \
                    FROM (SELECT user_id, SUM(total) FROM orders \
                    GROUP BY user_id HAVING (SUM(total) > 10000)) AS t \
                    JOIN users AS u ON (t.user_id = u.id) \
                    ORDER BY t.user_id""",
                    q.sql()
            );
        }
    }

    @Nested
    class Validation {

        @Test
        void noProjectionsThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> SelectBuilder.select(new Expr[0]));
        }

        @Test
        void nullProjectionThrows() {
            assertThrows(NullPointerException.class,
                    () -> SelectBuilder.select((Expr) null));
        }

        @Test
        void emptyGroupByThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .groupBy(new Expr[0])
            );
        }

        @Test
        void emptyOrderByThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .orderBy(new Expr[0])
            );
        }

        @Test
        void nullWhereThrows() {
            assertThrows(NullPointerException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .where(null)
            );
        }

        @Test
        void nullFromTableRefThrows() {
            assertThrows(NullPointerException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from((TableRef) null)
            );
        }

        @Test
        void nullFromSubqueryThrows() {
            assertThrows(NullPointerException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from(null, "alias")
            );
        }

        @Test
        void nullJoinAliasThrows() {
            var sub = SelectBuilder.select(col("id")).from("users").build();
            assertThrows(NullPointerException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .join(new Subquery.Table(sub), null, eq(col("id"), col("id")))
            );
        }
    }
    
    @Nested
    class TableValidation {
    
        @Test
        void nonExistentTable_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("ghost_table")
                            .build()
            );
        }
    
        @Test
        void nonExistentTableInJoin_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("u", "id"))
                            .from(new TableRef.AliasedBase("users", "u"))
                            .join(new TableRef.AliasedBase("ghost_table", "g"),
                                    eq(col("u", "id"), col("g", "id")))
                            .build()
            );
        }
    }
    
    @Nested
    class ProjectionValidation {
    
        @Test
        void nonExistentColumn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("ghost_column"))
                            .from("users")
                            .build()
            );
        }
    
        @Test
        void validColumn_passes() {
            assertDoesNotThrow(() ->
                    SelectBuilder.select(col("id"), col("name"))
                            .from("users")
                            .build()
            );
        }
    
        @Test
        void ambiguousColumn_fails() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from(new TableRef.AliasedBase("users", "u"))
                            .join(new TableRef.AliasedBase("orders", "o"),
                                    eq(col("u", "id"), col("o", "user_id")))
                            .build()
            );
        }
    
        @Test
        void qualifiedColumnsResolveAmbiguity_passes() {
            assertDoesNotThrow(() ->
                    SelectBuilder.select(colAs("u", "id", "user_id"), colAs("o", "id", "order_id"))
                            .from(new TableRef.AliasedBase("users", "u"))
                            .join(new TableRef.AliasedBase("orders", "o"),
                                    eq(col("u", "id"), col("o", "user_id")))
                            .build()
            );
        }
    }
    
    @Nested
    class WhereValidation {
    
        @Test
        void nonExistentColumnInWhere_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .where(eq(col("ghost_column"), new Literal.IntLiteral(1)))
                            .build()
            );
        }
    
        @Test
        void validColumnInWhere_passes() {
            assertDoesNotThrow(() ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .where(eq(col("name"), new Literal.StringLiteral("Alice")))
                            .build()
            );
        }
    
        @Test
        void nonExistentColumnInComplexWhere_throws() {
            var where = new BinaryOp(AND,
                    eq(col("id"), new Literal.IntLiteral(1)),
                    eq(col("ghost_column"), new Literal.IntLiteral(2)));
    
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .where(where)
                            .build()
            );
        }
    }
    
    @Nested
    class JoinValidation {
    
        @Test
        void nonExistentColumnInJoinOn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(colAs("u", "id", "uid"))
                            .from(new TableRef.AliasedBase("users", "u"))
                            .join(new TableRef.AliasedBase("orders", "o"),
                                    eq(col("u", "ghost_column"), col("o", "user_id")))
                            .build()
            );
        }
    
        @Test
        void validJoinOn_passes() {
            assertDoesNotThrow(() ->
                    SelectBuilder.select(colAs("u", "id", "uid"), colAs("o", "id", "oid"))
                            .from(new TableRef.AliasedBase("users", "u"))
                            .join(new TableRef.AliasedBase("orders", "o"),
                                    eq(col("u", "id"), col("o", "user_id")))
                            .build()
            );
        }
    }
    
    @Nested
    class GroupByValidation {
    
        @Test
        void nonExistentColumnInGroupBy_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("status"), new Func.CountAll())
                            .from("orders")
                            .groupBy(col("ghost_column"))
                            .build()
            );
        }
    
        @Test
        void validColumnInGroupBy_passes() {
            assertDoesNotThrow(() ->
                    SelectBuilder.select(col("status"), new Func.CountAll())
                            .from("orders")
                            .groupBy(col("status"))
                            .build()
            );
        }
    }
    
    @Nested
    class OrderByValidation {
    
        @Test
        void nonExistentColumnInOrderBy_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("id"))
                            .from("users")
                            .orderBy(col("ghost_column"))
                            .build()
            );
        }
    
        @Test
        void validColumnInOrderBy_passes() {
            assertDoesNotThrow(() ->
                    SelectBuilder.select(col("id"), col("name"))
                            .from("users")
                            .orderBy(col("name"))
                            .build()
            );
        }
    }
    
    @Nested
    class OuterContext {
    
        private JQ.Read outerSelect() {
            return SelectBuilder.select(col("u", "id"), col("u", "name"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();
        }
    
        @Test
        void columnFromOuterContext_passes() {
            var outer = outerSelect();
    
            var sub = SelectBuilder.select(col("o", "id"))
                    .from(new TableRef.AliasedBase("orders", "o"))
                    .where(eq(col("o", "user_id"), col("u", "id")))
                    .build(outer);
    
            assertDoesNotThrow(() ->
                    SelectBuilder.select(col("u", "id"), col("u", "name"))
                            .from(new TableRef.AliasedBase("users", "u"))
                            .where(new Exists(new Subquery.Table(sub)))
                            .build()
            );
        }
    
        @Test
        void ghostColumnNotInOuterContext_fails() {
            var outer = outerSelect();
    
            assertThrows(SchemaVerificationException.class, () ->
                    SelectBuilder.select(col("o", "id"))
                            .from(new TableRef.AliasedBase("orders", "o"))
                            .where(eq(col("o", "user_id"), col("u", "ghost_column")))
                            .build(outer)
            );
        }
    
        @Test
        void twoLevelsOuter_columnFromTwoLevelsUp_passes() {
            var level2 = SelectBuilder.select(col("u", "id"), col("u", "name"))
                    .from(new TableRef.AliasedBase("users", "u"))
                    .build();
    
            var level1 = SelectBuilder.select(col("o", "id"))
                    .from(new TableRef.AliasedBase("orders", "o"))
                    .build(level2);
    
            assertDoesNotThrow(() ->
                    SelectBuilder.select(col("i", "id"))
                            .from(new TableRef.AliasedBase("order_items", "i"))
                            .where(eq(col("i", "order_id"), col("o", "id")))
                            .build(level1)
            );
        }
    }
}