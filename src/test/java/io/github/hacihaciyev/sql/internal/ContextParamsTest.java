package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.builders.*;
import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.internal.value_objects.ParamType;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;
import static scala.jdk.javaapi.CollectionConverters.asJava;

@ExtendWith(DBTestContainer.class)
class ContextParamsTest {

    public record Username(String value) {}
    public record Email(String value) {}
    public record Password(String value) {}
    public record UserId(Integer value) {}
    public record OrderId(Long value) {}
    public record ProductId(Integer value) {}
    public record Amount(BigDecimal value) {}
    public record Quantity(Integer value) {}
    public record Status(String value) {}
    public record Department(String value) {}

    private static List<ParamType> paramTypes(JQ jq) {
        var result = switch (jq) {
            case JQ.Read(_, var context)  -> ((Context) context).paramTypes();
            case JQ.Write(_, var context) -> ((Context) context).paramTypes();
        };
        return asJava(result);
    }

    private static void assertTypes(List<ParamType> params, Class<?>... expected) {
        assertEquals(expected.length, params.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(i + 1,       params.get(i).position());
            assertEquals(expected[i], params.get(i)._type());
        }
    }

    @Nested
    class InsertContext {

        @Test
        void userRegistration() {
            var jq = new InsertBuilder("users")
                .columns(
                    "name",   Username.class,
                    "email",  Email.class,
                    "active", Boolean.class
                )
                .build();

            assertTypes(paramTypes(jq), Username.class, Email.class, Boolean.class);
        }

        @Test
        void upsert_onConflict_email_updateName() {
            var jq = new InsertBuilder("users")
                .columns(
                    "name",  Username.class,
                    "email", Email.class
                )
                .onConflict("email")
                .update("name")
                .build();

            assertTypes(paramTypes(jq), Username.class, Email.class);
        }

        @Test
        void orderInsert_withReturning_doesNotAddParams() {
            var jq = new InsertBuilder("orders")
                .columns(
                    "user_id", UserId.class,
                    "total",   Amount.class,
                    "status",  Status.class
                )
                .returning("id", "user_id", "total")
                .build();

            assertTypes(paramTypes(jq), UserId.class, Amount.class, Status.class);
        }

        @Test
        void orderItemInsert_allColumns() {
            var jq = new InsertBuilder("order_items")
                .columns(
                    "id",        Integer.class,
                    "order_id",  OrderId.class,
                    "product",   Username.class,
                    "qty",       Quantity.class
                )
                .build();

            assertTypes(paramTypes(jq), Integer.class, OrderId.class, Username.class, Quantity.class);
        }

        @Test
        void insert_manyColumns_exactOrderVerified() {
            var jq = new InsertBuilder("orders")
                .columns(
                    "user_id", UserId.class,
                    "total",   Amount.class,
                    "status",  Status.class,
                    "amount",  Amount.class
                )
                .build();

            assertTypes(paramTypes(jq), UserId.class, Amount.class, Status.class, Amount.class);
        }
    }

    @Nested
    class UpdateContext {

        @Test
        void updateEmailAndName_whereId() {
            var jq = new UpdateBuilder("users")
                .set("email", Email.class, "name", Username.class)
                .where(eq(col("id"), param(UserId.class)))
                .build();

            assertTypes(paramTypes(jq), Email.class, Username.class, UserId.class);
        }

        @Test
        void incrementQuantity_withLiteralStep_noExtraParams() {
            var jq = new UpdateBuilder("order_items")
                .set("qty", add(col("qty"), lit(1)))
                .where(eq(col("id"), param(ProductId.class)))
                .build();

            assertTypes(paramTypes(jq), ProductId.class);
        }

        @Test
        void incrementQuantity_withParamStep_includesStepParam() {
            var jq = new UpdateBuilder("order_items")
                .set("qty", add(col("qty"), param(Quantity.class)))
                .where(eq(col("id"), param(ProductId.class)))
                .build();

            assertTypes(paramTypes(jq), Quantity.class, ProductId.class);
        }

        @Test
        void mixed_param_computed_param_preservesInterleaving() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "status",  Status.class,
                    "total",   multiply(col("total"), param(Amount.class)),
                    "user_id", UserId.class
                )
                .build();

            assertTypes(paramTypes(jq), Status.class, Amount.class, UserId.class);
        }

        @Test
        void computed_param_computed_interleaving() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "total",   multiply(col("total"), param(Amount.class)),
                    "user_id", UserId.class,
                    "status",  add(col("amount"), param(Quantity.class))
                )
                .build();

            assertTypes(paramTypes(jq), Amount.class, UserId.class, Quantity.class);
        }

        @Test
        void literalInComputedExpr_noPlaceholder_skipped() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "status", Status.class,
                    "total",  multiply(col("total"), lit(2))
                )
                .where(eq(col("id"), param(OrderId.class)))
                .build();

            assertTypes(paramTypes(jq), Status.class, OrderId.class);
        }

        @Test
        void whereWithAndCondition_bothPlaceholders() {
            var jq = new UpdateBuilder("users")
                .set("name", Username.class)
                .where(and(
                    eq(col("id"),     param(UserId.class)),
                    eq(col("email"),  param(Email.class)),
                    eq(col("active"), lit(true))
                ))
                .build();

            assertTypes(paramTypes(jq), Username.class, UserId.class, Email.class);
        }

        @Test
        void whereWithBetween_placeholderBounds() {
            var jq = new UpdateBuilder("orders")
                .set("status", Status.class)
                .where(between(col("total"), param(Amount.class), param(Amount.class)))
                .build();

            assertTypes(paramTypes(jq), Status.class, Amount.class, Amount.class);
        }

        @Test
        void whereWithIn_placeholders() {
            var jq = new UpdateBuilder("orders")
                .set("status", Status.class)
                .where(in(col("user_id"),
                    param(UserId.class),
                    param(UserId.class),
                    param(UserId.class)
                ))
                .build();

            assertTypes(paramTypes(jq), Status.class, UserId.class, UserId.class, UserId.class);
        }

        @Test
        void manyMixed_fiveSetParams_twoWhereParams() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "status",  Status.class,
                    "total",   multiply(col("total"), param(Amount.class)),
                    "user_id", UserId.class,
                    "amount",  add(col("amount"), param(Quantity.class))
                )
                .where(and(
                    eq(col("id"),      param(OrderId.class)),
                    eq(col("user_id"), param(UserId.class))
                ))
                .build();

            assertTypes(paramTypes(jq),
                Status.class, Amount.class, UserId.class, Quantity.class,
                OrderId.class, UserId.class
            );
        }

        @Test
        void allComputed_withNestedPlaceholders() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "total",  add(multiply(col("total"), param(Amount.class)), param(Amount.class)),
                    "amount", subtract(col("amount"), param(Quantity.class))
                )
                .where(eq(col("id"), param(OrderId.class)))
                .build();

            assertTypes(paramTypes(jq), Amount.class, Amount.class, Quantity.class, OrderId.class);
        }

        @Test
        void alternating_literal_param_param_literal_computed() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "status",  Status.class,
                    "total",   multiply(col("total"), lit(2)),
                    "user_id", UserId.class,
                    "amount",  add(col("amount"), param(Amount.class))
                )
                .where(and(
                    eq(col("id"),     param(OrderId.class)),
                    eq(col("status"), lit("active"))
                ))
                .build();

            assertTypes(paramTypes(jq), Status.class, UserId.class, Amount.class, OrderId.class);
        }
    }

    @Nested
    class DeleteContext {

        @Test
        void deleteByUserId() {
            var jq = new DeleteBuilder("users")
                .where(eq(col("id"), param(UserId.class)))
                .build();

            assertTypes(paramTypes(jq), UserId.class);
        }

        @Test
        void deleteByEmailAndStatus() {
            var jq = new DeleteBuilder("users")
                .where(and(
                    eq(col("email"),  param(Email.class)),
                    eq(col("active"), lit(false))
                ))
                .build();

            assertTypes(paramTypes(jq), Email.class);
        }

        @Test
        void deleteByIdRange_preservesOrder() {
            var jq = new DeleteBuilder("orders")
                .where(between(col("id"), param(OrderId.class), param(OrderId.class)))
                .build();

            assertTypes(paramTypes(jq), OrderId.class, OrderId.class);
        }

        @Test
        void deleteInList_threeUserIds() {
            var jq = new DeleteBuilder("users")
                .where(in(col("id"),
                    param(UserId.class),
                    param(UserId.class),
                    param(UserId.class)
                ))
                .build();

            assertTypes(paramTypes(jq), UserId.class, UserId.class, UserId.class);
        }

        @Test
        void deleteWithLiteralOnly_emptyParams() {
            var jq = new DeleteBuilder("users")
                .where(eq(col("active"), lit(false)))
                .build();

            assertTypes(paramTypes(jq));
        }

        @Test
        void deleteWithMixedLiteralAndParam() {
            var jq = new DeleteBuilder("orders")
                .where(and(
                    eq(col("status"),  lit("expired")),
                    eq(col("user_id"), param(UserId.class))
                ))
                .build();

            assertTypes(paramTypes(jq), UserId.class);
        }

        @Test
        void deleteManyConditions_mixedLiteralsAndParams() {
            var jq = new DeleteBuilder("orders")
                .where(and(
                    eq(col("status"),  lit("expired")),
                    eq(col("user_id"), param(UserId.class)),
                    between(col("total"), param(Amount.class), param(Amount.class)),
                    in(col("id"),
                        param(OrderId.class),
                        param(OrderId.class)
                    )
                ))
                .build();

            assertTypes(paramTypes(jq),
                UserId.class, Amount.class, Amount.class,
                OrderId.class, OrderId.class
            );
        }

        @Test
        void deleteComplexNestedAnd_sevenParams() {
            var jq = new DeleteBuilder("orders")
                .where(and(
                    and(
                        eq(col("user_id"), param(UserId.class)),
                        eq(col("status"),  param(Status.class))
                    ),
                    and(
                        between(col("total"), param(Amount.class), param(Amount.class)),
                        in(col("id"),
                            param(OrderId.class),
                            param(OrderId.class),
                            param(OrderId.class)
                        )
                    )
                ))
                .build();

            assertTypes(paramTypes(jq),
                UserId.class, Status.class,
                Amount.class, Amount.class,
                OrderId.class, OrderId.class, OrderId.class
            );
        }
    }

    @Nested
    class SelectContext {

        @Test
        void noPlaceholders_emptyParams() {
            var jq = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .build();

            assertTypes(paramTypes(jq));
        }

        @Test
        void whereByEmail() {
            var jq = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            assertTypes(paramTypes(jq), Email.class);
        }

        @Test
        void whereMixedLiteralAndParam_onlyParamCounted() {
            var jq = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(and(
                    eq(col("active"), lit(true)),
                    eq(col("email"),  param(Email.class))
                ))
                .build();

            assertTypes(paramTypes(jq), Email.class);
        }

        @Test
        void joinOn_before_where_preservesOrder() {
            var jq = SelectBuilder
                .select(colAs("u", "id", "uid"), colAs("o", "id", "oid"))
                .from(tAs("users", "u"))
                .join(tAs("orders", "o"),
                    and(
                        eq(col("u", "id"),    col("o", "user_id")),
                        gt(col("o", "total"), param(Amount.class))
                    ))
                .where(eq(col("u", "email"), param(Email.class)))
                .build();

            assertTypes(paramTypes(jq), Amount.class, Email.class);
        }

        @Test
        void multipleJoins_preservesJoinThenWhereOrder() {
            var jq = SelectBuilder
                .select(
                    colAs("u", "id", "uid"),
                    colAs("o", "id", "oid"),
                    colAs("i", "id", "iid"))
                .from(tAs("users", "u"))
                .join(tAs("orders", "o"),
                    and(
                        eq(col("u", "id"),    col("o", "user_id")),
                        gt(col("o", "total"), param(Amount.class))
                    ))
                .join(tAs("order_items", "i"),
                    and(
                        eq(col("o", "id"),  col("i", "order_id")),
                        eq(col("i", "qty"), param(Quantity.class))
                    ))
                .where(eq(col("u", "email"), param(Email.class)))
                .build();

            assertTypes(paramTypes(jq), Amount.class, Quantity.class, Email.class);
        }

        @Test
        void havingWithPlaceholder() {
            var jq = SelectBuilder.select(col("department"), avg(col("salary")))
                .from("employees")
                .where(eq(col("active"), lit(true)))
                .groupBy(col("department"))
                .having(gt(avg(col("salary")), param(Amount.class)))
                .build();

            assertTypes(paramTypes(jq), Amount.class);
        }

        @Test
        void where_before_having_preservesOrder() {
            var jq = SelectBuilder.select(col("department"), countAll())
                .from("employees")
                .where(eq(col("department"), param(Department.class)))
                .groupBy(col("department"))
                .having(gt(countAll(), param(Quantity.class)))
                .build();

            assertTypes(paramTypes(jq), Department.class, Quantity.class);
        }

        @Test
        void caseExprWithPlaceholdersInProjection() {
            var jq = SelectBuilder.select(
                    col("id"),
                    caseWhen(
                        lit("unknown"),
                        when(eq(col("status"), param(Status.class)), lit("active")),
                        when(eq(col("status"), param(Status.class)), lit("inactive"))
                    ))
                .from("orders")
                .build();

            assertTypes(paramTypes(jq), Status.class, Status.class);
        }

        @Test
        void twoJoins_complexOnConditions_complexWhere_eightParams() {
            var jq = SelectBuilder
                .select(
                    colAs("u", "id", "uid"),
                    colAs("o", "id", "oid"),
                    colAs("i", "id", "iid"))
                .from(tAs("users", "u"))
                .join(tAs("orders", "o"),
                    and(
                        eq(col("u", "id"),    col("o", "user_id")),
                        gt(col("o", "total"), param(Amount.class)),
                        eq(col("o", "status"), param(Status.class))
                    ))
                .join(tAs("order_items", "i"),
                    and(
                        eq(col("o", "id"),    col("i", "order_id")),
                        gt(col("i", "qty"),   param(Quantity.class)),
                        lt(col("i", "qty"),   param(Quantity.class))
                    ))
                .where(and(
                    eq(col("u", "email"),  param(Email.class)),
                    eq(col("u", "active"), lit(true)),
                    between(col("o", "total"), param(Amount.class), param(Amount.class))
                ))
                .build();

            assertTypes(paramTypes(jq),
                Amount.class, Status.class,
                Quantity.class, Quantity.class,
                Email.class, Amount.class, Amount.class
            );
        }

        @Test
        void joinWhereGroupByHaving_sixParams() {
            var jq = SelectBuilder
                .select(colAs("u", "id", "uid"), colAs("o", "id", "oid"), sum(col("o", "total")))
                .from(tAs("users", "u"))
                .join(tAs("orders", "o"),
                    and(
                        eq(col("u", "id"),     col("o", "user_id")),
                        gt(col("o", "total"),  param(Amount.class))
                    ))
                .where(and(
                    eq(col("u", "email"),  param(Email.class)),
                    eq(col("u", "active"), lit(true)),
                    in(col("o", "status"),
                        param(Status.class),
                        param(Status.class)
                    )
                ))
                .groupBy(col("u", "id"), col("o", "id"))
                .having(and(
                    gt(sum(col("o", "total")), param(Amount.class)),
                    lt(sum(col("o", "total")), param(Amount.class))
                ))
                .build();

            assertTypes(paramTypes(jq),
                Amount.class,
                Email.class, Status.class, Status.class,
                Amount.class, Amount.class
            );
        }
    }

    @Nested
    class UnionContext {

        private JQ.Read activeUsers() {
            return SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), lit(true)))
                .build();
        }

        private JQ.Read inactiveUsers() {
            return SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), lit(false)))
                .build();
        }

        @Test
        void noPlaceholders_emptyParams() {
            var jq = new UnionBuilder(UnionType.UNION, activeUsers(), inactiveUsers()).build();

            assertTypes(paramTypes(jq));
        }

        @Test
        void placeholderOnlyInFirstQuery() {
            var first = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            var jq = new UnionBuilder(UnionType.UNION, first, inactiveUsers()).build();

            assertTypes(paramTypes(jq), Email.class);
        }

        @Test
        void placeholdersInBothQueries_differentTypes_preservesOrder() {
            var first = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("id"), param(UserId.class)))
                .build();

            var second = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            var jq = new UnionBuilder(UnionType.UNION, first, second).build();

            assertTypes(paramTypes(jq), UserId.class, Email.class);
        }

        @Test
        void threeQueries_complexParams_preservesOrder() {
            var q1 = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(and(
                    eq(col("id"),    param(UserId.class)),
                    eq(col("email"), param(Email.class))
                ))
                .build();

            var q2 = SelectBuilder.select(col("id"), col("status"))
                .from("orders")
                .where(eq(col("id"), param(OrderId.class)))
                .build();

            var q3 = SelectBuilder.select(col("id"), col("product"))
                .from("order_items")
                .where(between(col("qty"), param(Quantity.class), param(Quantity.class)))
                .build();

            var jq = new UnionBuilder(UnionType.UNION, q1, q2).add(q3).build();

            assertTypes(paramTypes(jq),
                UserId.class, Email.class,
                OrderId.class,
                Quantity.class, Quantity.class
            );
        }

        @Test
        void mixedLiteralAndParam_acrossQueries() {
            var first = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(and(
                    eq(col("active"), lit(true)),
                    eq(col("email"),  param(Email.class))
                ))
                .build();

            var second = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(and(
                    eq(col("active"), lit(false)),
                    eq(col("id"),     param(UserId.class))
                ))
                .build();

            var jq = new UnionBuilder(UnionType.UNION_ALL, first, second).build();

            assertTypes(paramTypes(jq), Email.class, UserId.class);
        }

        @Test
        void threeQueries_manyParamsEach_tenTotal() {
            var q1 = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(and(
                    eq(col("id"),    param(UserId.class)),
                    eq(col("email"), param(Email.class)),
                    eq(col("name"),  param(Username.class))
                ))
                .build();

            var q2 = SelectBuilder.select(col("id"), col("status"))
                .from("orders")
                .where(and(
                    between(col("total"), param(Amount.class), param(Amount.class)),
                    eq(col("user_id"),   param(UserId.class))
                ))
                .build();

            var q3 = SelectBuilder.select(col("id"), col("product"))
                .from("order_items")
                .where(and(
                    in(col("id"),
                        param(ProductId.class),
                        param(ProductId.class)
                    ),
                    between(col("qty"), param(Quantity.class), param(Quantity.class))
                ))
                .build();

            var jq = new UnionBuilder(UnionType.UNION_ALL, q1, q2).add(q3).build();

            assertTypes(paramTypes(jq),
                UserId.class, Email.class, Username.class,
                Amount.class, Amount.class, UserId.class,
                ProductId.class, ProductId.class, Quantity.class, Quantity.class
            );
        }
    }

    @Nested
    class SubqueryPlaceholders {

        private JQ.Read allUsers() {
            return SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .build();
        }

        private JQ.Read finalById() {
            return SelectBuilder.select(col("id"))
                .from("users")
                .where(eq(col("id"), param(UserId.class)))
                .build();
        }

        @Test
        void fromSubquery_ownParam_notCountedByOuter() {
            var sub = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build();

            var jq = SelectBuilder.select(col("id"))
                .from(new Subquery.Table(sub), "active_users")
                .build();

            assertTypes(paramTypes(jq));
        }

        @Test
        void inSubquery_ownParam_nowCounted() {
            var sub = SelectBuilder.select(col("user_id"))
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var jq = SelectBuilder.select(col("id"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                .build();

            assertTypes(paramTypes(jq), Amount.class);
        }

        @Test
        void existsSubquery_ownParam_nowCounted_outerCorrelationStillIgnored() {
            var outer = SelectBuilder.select(col("u", "id"))
                .from(new TableRef.AliasedBase("users", "u"))
                .build();

            var sub = SelectBuilder.select(col("o", "id"))
                .from(new TableRef.AliasedBase("orders", "o"))
                .where(and(
                    eq(col("o", "user_id"), col("u", "id")),
                    gt(col("o", "total"), param(Amount.class))
                ))
                .build(outer);

            var jq = SelectBuilder.select(col("u", "id"))
                .from(new TableRef.AliasedBase("users", "u"))
                .where(new Exists(new Subquery.Table(sub)))
                .build();

            assertTypes(paramTypes(jq), Amount.class);
        }

        @Test
        void scalarSubquery_ownParam_nowCounted() {
            var sub = SelectBuilder.select(countAll())
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var jq = SelectBuilder.select(col("name"), new Subquery.Scalar(sub))
                .from("users")
                .build();

            assertTypes(paramTypes(jq), Amount.class);
        }

        @Test
        void outerHasOwnParam_subqueryParamNowCounted_bothPresentInOrder() {
            var sub = SelectBuilder.select(col("user_id"))
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var jq = SelectBuilder.select(col("id"))
                .from("users")
                .where(and(
                    eq(col("id"), param(UserId.class)),
                    new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub)))
                ))
                .build();

            assertTypes(paramTypes(jq), UserId.class, Amount.class);
        }

        @Test
        void quantifiedAll_subqueryParam_nowCounted() {
            var sub = SelectBuilder.select(col("total"))
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var jq = SelectBuilder.select(col("id"))
                .from("orders")
                .where(new QuantifiedExpr.All(
                    QuantifiedExpr.ComparisonOperator.GT, col("total"), new Subquery.Table(sub)))
                .build();

            assertTypes(paramTypes(jq), Amount.class);
        }

        @Test
        void nestedInsideCte_subqueryParamNowCounted() {
            var sub = SelectBuilder.select(col("user_id"))
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var withQuery = SelectBuilder.select(col("id"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))))
                .build();

            var jq = new CTEBuilder("filtered", withQuery)
                .build(SelectBuilder.select(col("id")).from("users").build());

            assertTypes(paramTypes(jq), Amount.class);
        }

        @Test
        void nestedSubqueryInsideSubquery_bothLevelsCounted_innerFirst() {
            // WHERE id IN (SELECT user_id FROM orders WHERE total > ? AND status IN (SELECT product FROM order_items WHERE product = ?))
            var innermost = SelectBuilder.select(col("product"))
                .from("order_items")
                .where(eq(col("product"), param(Username.class)))
                .build();

            var middle = SelectBuilder.select(col("user_id"))
                .from("orders")
                .where(and(
                    gt(col("total"), param(Amount.class)),
                    new InExpr.In(col("status"), new InExpr.SubquerySource(new Subquery.Table(innermost)))
                ))
                .build();

            var jq = SelectBuilder.select(col("id"))
                .from("users")
                .where(new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(middle))))
                .build();

            // Rendering order inside `middle`'s own WHERE: `total > ?` (Amount) before the nested IN's `?` (Username)
            assertTypes(paramTypes(jq), Amount.class, Username.class);
        }

        @Test
        void sameSubqueryInstanceUsedTwice_paramsCountedOncePerOccurrence() {
            var sub = SelectBuilder.select(col("user_id"))
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var jq = SelectBuilder.select(col("id"))
                .from("users")
                .where(or(
                    new InExpr.In(col("id"), new InExpr.SubquerySource(new Subquery.Table(sub))),
                    new Exists(new Subquery.Table(sub))
                ))
                .build();

            assertTypes(paramTypes(jq), Amount.class, Amount.class);
        }

        @Test
        void unionQuery_oneSideHasEmbeddedSubqueryParam_countedWithinThatSide() {
            var sub = SelectBuilder.select(col("total"))
                .from("orders")
                .where(gt(col("total"), param(Amount.class)))
                .build();

            var first = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(new Exists(new Subquery.Table(sub)))
                .build();

            var second = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            var jq = new UnionBuilder(UnionType.UNION, first, second).build();

            assertTypes(paramTypes(jq), Amount.class, Email.class);
        }

        @Test
        void nestedCteInsideCte_outerCteParamsPrecedeInnerFinal() {
            var innerWith = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            var innerFinal = SelectBuilder.select(col("id"))
                .from("users")
                .build();

            var innerCte = new CTEBuilder("inner_cte", innerWith).build(innerFinal);

            var outerCte = new CTEBuilder("outer_cte", innerCte)
                .build(finalById());

            assertTypes(paramTypes(outerCte), Email.class, UserId.class);
        }

        @Test
        void sameWithQueryInstanceUsedTwice_paramsCountedOncePerEntry() {
            var withQuery = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            var jq = new CTEBuilder("a", withQuery)
                .with("b", withQuery)
                .build(finalById());

            assertTypes(paramTypes(jq), Email.class, Email.class, UserId.class);
        }

        @Test
        void recursiveCte_unionTypeDoesNotAffectParamAggregation() {
            var base = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build();

            var recursive = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Department.class)))
                .build();

            var jq = new CTEBuilder("seed", allUsers())
                .withRecursive("tree", base, recursive, UnionType.INTERSECT)
                .build(finalById());

            assertTypes(paramTypes(jq), Boolean.class, Department.class, UserId.class);
        }

        @Test
        void multipleRecursiveEntries_bothContributeInDeclarationOrder() {
            var base1 = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("email"), param(Email.class)))
                .build();

            var rec1 = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("active"), param(Boolean.class)))
                .build();

            var base2 = SelectBuilder.select(col("id"), col("status"))
                .from("orders")
                .where(eq(col("status"), param(Username.class)))
                .build();

            var rec2 = SelectBuilder.select(col("id"), col("status"))
                .from("orders")
                .where(eq(col("id"), param(UserId.class)))
                .build();

            var jq = new CTEBuilder("x", allUsers())
                .withRecursive("a", base1, rec1)
                .withRecursive("b", base2, rec2)
                .build(finalById());

            assertTypes(paramTypes(jq),
                Email.class, Boolean.class, Username.class, UserId.class, UserId.class);
        }
    }

    @Nested
    class OuterAndParamTypes {

        private JQ.Read ordersOuter() {
            return SelectBuilder.select(col("id"), col("status"))
                .from("orders")
                .build();
        }

        @Test
        void insertWithOuter_paramTypesOnlyOwnColumns() {
            var outer = ordersOuter();

            var jq = new InsertBuilder("users")
                .columns("name", Username.class, "email", Email.class)
                .build(outer);

            assertTypes(paramTypes(jq), Username.class, Email.class);
        }

        @Test
        void updateWithOuter_paramTypesOnlyOwnSetAndWhere() {
            var outer = ordersOuter();

            var jq = new UpdateBuilder("users")
                .set("name", Username.class)
                .where(eq(col("id"), param(UserId.class)))
                .build(outer);

            assertTypes(paramTypes(jq), Username.class, UserId.class);
        }

        @Test
        void deleteWithOuter_paramTypesOnlyOwnWhere() {
            var outer = ordersOuter();

            var jq = new DeleteBuilder("users")
                .where(eq(col("id"), param(UserId.class)))
                .build(outer);

            assertTypes(paramTypes(jq), UserId.class);
        }

        @Test
        void selectWithOuter_paramTypesOnlyOwnWhere() {
            var outer = ordersOuter();

            var jq = SelectBuilder.select(col("id"), col("name"))
                .from("users")
                .where(eq(col("id"), param(UserId.class)))
                .build(outer);

            assertTypes(paramTypes(jq), UserId.class);
        }

        @Test
        void twoLevelsOuter_paramTypesStillOnlyInnermostOwnParams() {
            var level2 = ordersOuter();

            var level1 = SelectBuilder.select(col("id"))
                .from("order_items")
                .where(eq(col("order_id"), param(UserId.class)))
                .build(level2);

            var jq = new InsertBuilder("users")
                .columns("name", Username.class)
                .build(level1);

            assertTypes(paramTypes(jq), Username.class);
        }

        @Test
        void outerItselfHasParams_notCountedInDependent() {
            var outer = SelectBuilder.select(col("id"))
                .from("orders")
                .where(eq(col("status"), param(Username.class)))
                .build();

            var jq = new DeleteBuilder("users")
                .where(eq(col("id"), param(UserId.class)))
                .build(outer);

            assertTypes(paramTypes(jq), UserId.class);
        }
    }
}