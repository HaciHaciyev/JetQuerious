package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.DBTestContainer;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicLong;

import static io.github.hacihaciyev.sql.QueryForge.*;
import static io.github.hacihaciyev.sql.SQL.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class DeconstructionBindingTest {

    private static final AtomicLong ID_SEQ = new AtomicLong(30_000);

    private JetQuerious jq;

    @BeforeEach
    void setUp() {
        jq = JetQuerious.defaultInstance();
        jq.write(deleteFrom("order_items").build());
        jq.write(deleteFrom("orders").build());
        jq.write(deleteFrom("users").build());
    }

    private static long nextId() {
        return ID_SEQ.getAndIncrement();
    }

    public record NameEmail(String name, String email) {}
    public record FullContact(String name, String email, String phone) {}
    public record ThreeStrings(String a, String b, String c) {}
    public record WrongLeadingType(Boolean flag, Long id) {}
    public record NameId(String name, Long id) {}

    private static JQ.Write insertUser() {
        return insertInto("users")
            .columns("id", Long.class, "name", String.class, "email", String.class, "active", Boolean.class)
            .build();
    }

    private static JQ.Read selectUserById() {
        return select(col("name"), col("email"))
            .from("users")
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static JQ.Write updateNameAndId() {
        return update("users")
            .set("name", String.class)
            .where(eq(col("id"), param(Long.class)))
            .build();
    }

    private static ResultSetExtractor<String> stringValue(String column) {
        return rs -> rs.getString(column);
    }

    @Test
    void deconstructionInMiddleOfArgs_expandsIntoMatchingPositions() {
        var id = nextId();
        var contact = new NameEmail("Alice", "alice@example.com");

        var result = jq.write(insertUser(), id, Deconstruction.dec(contact), true);

        assertInstanceOf(Ok.class, result);
        assertEquals(1, result.or(0));

        var name = jq.one(selectUserById(), stringValue("name"), id);
        assertInstanceOf(Ok.class, name);
        assertEquals("Alice", name.or((String) null));
    }

    @Test
    void deconstructionWithLimit_usesOnlyLeadingFieldsInOrder() {
        var id = nextId();
        var contact = new FullContact("Bob", "bob@example.com", "+1000");

        var result = jq.write(insertUser(), id, Deconstruction.dec(contact, 2), true);

        assertInstanceOf(Ok.class, result);
        assertEquals(1, result.or(0));

        var name = jq.one(selectUserById(), stringValue("name"), id);
        assertInstanceOf(Ok.class, name);
        assertEquals("Bob", name.or((String) null));
    }

    @Test
    void deconstructionAsSoleArgument_matchingFullRecord_bindsBothFields() {
        var id = nextId();
        jq.write(insertUser(), id, "Carl", "carl@example.com", true);

        var result = jq.write(updateNameAndId(), Deconstruction.dec(new NameId("Carla", id)));

        assertInstanceOf(Ok.class, result);
        assertEquals(1, result.or(0));

        var name = jq.one(selectUserById(), stringValue("name"), id);
        assertInstanceOf(Ok.class, name);
        assertEquals("Carla", name.or((String) null));
    }

    @Test
    void deconstructionExceedingRemainingParamSlots_returnsErr() {
        var result = jq.write(updateNameAndId(), Deconstruction.dec(new ThreeStrings("a", "b", "c")));

        assertInstanceOf(Err.class, result);
        var err = (Err<Integer, Exception>) result;
        assertTrue(err.err() instanceof IllegalArgumentException);
        assertTrue(err.err().getMessage().contains("needs 3 parameter(s)"));
    }

    @Test
    void deconstructionWithMismatchedLeadingFieldType_returnsErr() {
        var result = jq.write(updateNameAndId(), Deconstruction.dec(new WrongLeadingType(true, 1L), 1));

        assertInstanceOf(Err.class, result);
        var err = (Err<Integer, Exception>) result;
        assertTrue(err.err() instanceof IllegalArgumentException);
        assertTrue(err.err().getMessage().contains("expects java.lang.String"));
    }

    @Test
    void deconstructionMixedWithPlainTrailingArg_stillBindsCorrectOrder() {
        var id = nextId();

        var result = jq.write(insertUser(), Deconstruction.dec(new IdAndName(id, "Dana"), 2), "dana@example.com", true);

        assertInstanceOf(Ok.class, result);
        assertEquals(1, result.or(0));

        var name = jq.one(selectUserById(), stringValue("name"), id);
        assertInstanceOf(Ok.class, name);
        assertEquals("Dana", name.or((String) null));
    }

    public record IdAndName(Long id, String name) {}
}
