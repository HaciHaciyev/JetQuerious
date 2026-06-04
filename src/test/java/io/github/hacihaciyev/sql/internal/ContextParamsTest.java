package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.builders.*;
import io.github.hacihaciyev.sql.internal.value_objects.ParamType;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;

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
            case JQ.Read(_, var context) -> ((Context) context).paramTypes();
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
                    "name",     Username.class,
                    "email",    Email.class,
                    "active",   Boolean.class
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
                    "total",    multiply(col("total"), param(Amount.class)),
                    "user_id",  UserId.class,
                    "status",   add(col("amount"), param(Quantity.class))
                )
                .build();

            assertTypes(paramTypes(jq), Amount.class, UserId.class, Quantity.class);
        }

        @Test
        void literalInComputedExpr_noPlaceholder_skipped() {
            var jq = new UpdateBuilder("orders")
                .set(
                    "status",  Status.class,
                    "total",   multiply(col("total"), lit(2))
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
                        eq(col("u", "id"),     col("o", "user_id")),
                        gt(col("o", "total"),  param(Amount.class))
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
                        eq(col("o", "id"),   col("i", "order_id")),
                        eq(col("i", "qty"),  param(Quantity.class))
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
    }
}