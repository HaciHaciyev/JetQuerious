package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class MapperBuilderTest {

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    private JetQuerious jq;

    @BeforeEach
    void setUp() {
        jq = JetQuerious.defaultInstance();
        jq.write(deleteFrom("orders").build());
        jq.write(deleteFrom("users").build());
    }

    private static long nextId() {
        return ID_SEQ.getAndIncrement();
    }

    public record Email(String value) {}
    public record Status(String value) {}

    public record User(long id, String name, Email email, boolean active) {}
    public record Order(long id, long userId, BigDecimal total, Status status) {}
    public record UserView(long id, String name) {}

    private static JQ.Write insertUser() {
        return insertInto("users")
            .columns("id", Long.class, "name", String.class, "email", String.class, "active", Boolean.class)
            .build();
    }

    private static JQ.Write insertOrder() {
        return insertInto("orders")
            .columns("id", Long.class, "user_id", Long.class, "total", BigDecimal.class, "amount", BigDecimal.class, "status", String.class)
            .build();
    }

    private static JQ.Read selectUserById() {
        return select(col("id"), col("name"), col("email"), col("active"))
            .from("users")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Read selectUserNameAndId() {
        return select(col("id"), col("name"))
            .from("users")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Read selectOrderById() {
        return select(col("id"), col("user_id"), col("total"), col("status"))
            .from("orders")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private long insertTestUser(String name, String email, boolean active) {
        var id = nextId();
        jq.write(insertUser(), id, name, email, active);
        return id;
    }

    private long insertTestOrder(long userId, BigDecimal total, String status) {
        var id = nextId();
        jq.write(insertOrder(), id, userId, total, total, status);
        return id;
    }

    @Nested
    class DeclareExplicit {

        @Test
        void mapsAllPlainFields() {
            var id = insertTestUser("Alice", "alice@example.com", true);

            var mapper = Mapper.declare(UserView.class)
                .field("id", Long.class)
                .field("name", String.class)
                .build(selectUserNameAndId());

            var result = jq.one(selectUserNameAndId(), mapper, id);

            assertTrue(result.isOk());
            var view = result.or((UserView) null);
            assertEquals(id, view.id());
            assertEquals("Alice", view.name());
        }

        @Test
        void mapsValueObjectField() {
            var id = insertTestUser("Bob", "bob@example.com", true);

            var mapper = Mapper.declare(User.class)
                .field("id", Long.class)
                .field("name", String.class)
                .valueObject("email", Email.class)
                .field("active", Boolean.class)
                .build(selectUserById());

            var result = jq.one(selectUserById(), mapper, id);

            assertTrue(result.isOk());
            var user = result.or((User) null);
            assertEquals("Bob", user.name());
            assertEquals(new Email("bob@example.com"), user.email());
            assertTrue(user.active());
        }

        @Test
        void mapsOrderWithValueObjectStatus() {
            var userId  = insertTestUser("Carol", "carol@example.com", true);
            var orderId = insertTestOrder(userId, new BigDecimal("99.99"), "pending");

            var mapper = Mapper.declare(Order.class)
                .field("id", Long.class)
                .field("user_id", Long.class)
                .field("total", BigDecimal.class)
                .valueObject("status", Status.class)
                .build(selectOrderById());

            var result = jq.one(selectOrderById(), mapper, orderId);

            assertTrue(result.isOk());
            var order = result.or((Order) null);
            assertEquals(orderId, order.id());
            assertEquals(userId, order.userId());
            assertEquals(new Status("pending"), order.status());
        }

        @Test
        void unknownColumn_throwsAtBuildTime() {
            var ex = assertThrows(IllegalArgumentException.class, () ->
                Mapper.declare(UserView.class)
                    .field("id", Long.class)
                    .field("ghost_column", String.class)
                    .build(selectUserNameAndId())
            );

            assertTrue(ex.getMessage().contains("ghost_column"));
        }

        @Test
        void typeMismatch_throwsAtBuildTime() {
            var typedSelect = select(col("id", Long.class), col("name"))
                .from("users")
                .build();

            assertThrows(IllegalArgumentException.class, () ->
                Mapper.declare(UserView.class)
                    .field("id", String.class)
                    .field("name", String.class)
                    .build(typedSelect)
            );
        }

        @Test
        void wrongFieldCount_throwsAtBuildTime() {
            var ex = assertThrows(IllegalArgumentException.class, () ->
                Mapper.declare(UserView.class)
                    .field("id", Long.class)
                    .build(selectUserNameAndId())
            );

            assertTrue(ex.getMessage().contains("Expected"));
        }

        @Test
        void unsupportedValueObjectType_throwsAtBuildTime() {
            record NotAValueObject(String a, String b) {}

            assertThrows(IllegalArgumentException.class, () ->
                Mapper.declare(UserView.class)
                    .field("id", Long.class)
                    .valueObject("name", NotAValueObject.class)
                    .build(selectUserNameAndId())
            );
        }

        @Test
        void mapperIsReusableAcrossMultipleRows() {
            insertTestUser("Dave", "dave@example.com", true);
            insertTestUser("Eve", "eve@example.com", false);

            var mapper = Mapper.declare(UserView.class)
                .field("id", Long.class)
                .field("name", String.class)
                .build(select(col("id"), col("name")).from("users").build());

            var result = jq.many(select(col("id"), col("name")).from("users").build(), mapper);

            assertTrue(result.isOk());
            assertEquals(2, result.or(java.util.List.of()).size());
        }
    }

    @Nested
    class NamingConventionsFlat {

        @Test
        void mapsSimpleRecordAutomatically() {
            var id = insertTestUser("Frank", "frank@example.com", true);

            var mapper = Mapper.namingConventions(User.class).build(selectUserById());

            var result = jq.one(selectUserById(), mapper, id);

            assertTrue(result.isOk());
            var user = result.or((User) null);
            assertEquals("Frank", user.name());
            assertEquals(new Email("frank@example.com"), user.email());
        }

        @Test
        void mapsOrderAutomatically() {
            var userId  = insertTestUser("Grace", "grace@example.com", true);
            var orderId = insertTestOrder(userId, new BigDecimal("50.00"), "shipped");

            var aliasedSelect = select(col("id"), colAs("user_id", "userId"), col("total"), col("status"))
                .from("orders")
                .where(eq(col("id"), param(Long.class)))
                .build();

            var mapper = Mapper.namingConventions(Order.class).build(aliasedSelect);

            var result = jq.one(aliasedSelect, mapper, orderId);

            assertTrue(result.isOk());
            var order = result.or((Order) null);
            assertEquals(new Status("shipped"), order.status());
            assertEquals(0, new BigDecimal("50.00").compareTo(order.total()));
        }

        @Test
        void simpleFlatRecord_mapsWithoutExplicitDeclarations() {
            var id = insertTestUser("Hank", "hank@example.com", true);

            var mapper = Mapper.namingConventions(UserView.class)
                .build(selectUserNameAndId());

            var result = jq.one(selectUserNameAndId(), mapper, id);

            assertTrue(result.isOk());
            assertEquals("Hank", result.or((UserView) null).name());
        }

        @Test
        void unsupportedNestedRecord_throwsAtNamingConventionsTime() {
            record Address(String street, String city) {}
            record Customer(long id, Address address) {}

            assertThrows(IllegalArgumentException.class, () ->
                Mapper.namingConventions(Customer.class)
            );
        }

        @Test
        void missingColumnInProjection_throwsAtBuildTime() {
            var partialSelect = select(col("id"))
                .from("users")
                .where(eq(col("id"), param(Long.class)))
                .build();

            assertThrows(IllegalArgumentException.class, () ->
                Mapper.namingConventions(User.class).build(partialSelect)
            );
        }
    }

    @Nested
    class NamingConventionsNested {

        public record Personnel(Email email, Status status) {}
        public record UserWithPersonnel(long id, String name, Personnel personnel) {}

        @Test
        void nestedRecord_readsByFieldNameIgnoringParentColumn() {
            var userId  = insertTestUser("Ivy", "ivy@example.com", true);
            insertTestOrder(userId, new BigDecimal("10.00"), "pending");

            var select = select(col("u", "id"), col("u", "name"), col("u", "email"), col("o", "status"))
                .from(io.github.hacihaciyev.sql.SQL.tAs("users", "u"))
                .join(io.github.hacihaciyev.sql.SQL.tAs("orders", "o"),
                    eq(col("u", "id"), col("o", "user_id")))
                .where(eq(col("u", "id"), param(Long.class)))
                .build();

            var mapper = Mapper.declare(UserWithPersonnel.class)
                .field("id", Long.class)
                .field("name", String.class)
                .namingConventions("personnel", Personnel.class)
                .build(select);

            var result = jq.one(select, mapper, userId);

            assertTrue(result.isOk());
            var user = result.or((UserWithPersonnel) null);
            assertEquals("Ivy", user.name());
            assertEquals(new Email("ivy@example.com"), user.personnel().email());
            assertEquals(new Status("pending"), user.personnel().status());
        }
    }

    @Nested
    class ColumnAliases {

        public record IdName(long userId, String name) {}

        @Test
        void mapsAliasedColumn() {
            var id = insertTestUser("Judy", "judy@example.com", true);

            var aliased = select(colAs("id", "user_id"), col("name"))
                .from("users")
                .where(eq(col("id"), param(Long.class)))
                .build();

            var mapper = Mapper.declare(IdName.class)
                .field("user_id", Long.class)
                .field("name", String.class)
                .build(aliased);

            var result = jq.one(aliased, mapper, id);

            assertTrue(result.isOk());
            assertEquals(id, result.or((IdName) null).userId());
        }

        @Test
        void variableBaseColumn_usesOriginalNameNotPrefixed() {
            var id = insertTestUser("Karl", "karl@example.com", true);

            var withAlias = select(col("u", "id"), col("u", "name"))
                .from(io.github.hacihaciyev.sql.SQL.tAs("users", "u"))
                .where(eq(col("u", "id"), param(Long.class)))
                .build();

            var mapper = Mapper.declare(UserView.class)
                .field("id", Long.class)
                .field("name", String.class)
                .build(withAlias);

            var result = jq.one(withAlias, mapper, id);

            assertTrue(result.isOk());
            assertEquals("Karl", result.or((UserView) null).name());
        }
    }
}