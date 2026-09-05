package io.github.hacihaciyev.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeconstructionTest {

    public record PersonalData(String username, String firstName, String lastName, String email, String phone) {}

    public record SingleField(String onlyValue) {}

    @Test
    void dec_all_returnsAllFieldsInDeclarationOrder() {
        var data = new PersonalData("jdoe", "John", "Doe", "john@example.com", "+1000");

        var values = Deconstruction.dec(data).deconstruct();

        assertArrayEquals(new Object[]{"jdoe", "John", "Doe", "john@example.com", "+1000"}, values);
    }

    @Test
    void dec_withLimit_returnsOnlyLeadingFields() {
        var data = new PersonalData("jdoe", "John", "Doe", "john@example.com", "+1000");

        var values = Deconstruction.dec(data, 2).deconstruct();

        assertArrayEquals(new Object[]{"jdoe", "John"}, values);
    }

    @Test
    void dec_withLimitEqualToFieldCount_returnsAllFields() {
        var data = new PersonalData("jdoe", "John", "Doe", "john@example.com", "+1000");

        var values = Deconstruction.dec(data, 5).deconstruct();

        assertArrayEquals(new Object[]{"jdoe", "John", "Doe", "john@example.com", "+1000"}, values);
    }

    @Test
    void dec_withLimitGreaterThanFieldCount_throws() {
        var data = new PersonalData("jdoe", "John", "Doe", "john@example.com", "+1000");

        assertThrows(IllegalArgumentException.class, () -> Deconstruction.dec(data, 6).deconstruct());
    }

    @Test
    void dec_withZeroLimit_throwsAtConstruction() {
        var data = new PersonalData("jdoe", "John", "Doe", "john@example.com", "+1000");

        assertThrows(IllegalArgumentException.class, () -> Deconstruction.dec(data, 0));
    }

    @Test
    void dec_withNegativeLimit_throwsAtConstruction() {
        var data = new PersonalData("jdoe", "John", "Doe", "john@example.com", "+1000");

        assertThrows(IllegalArgumentException.class, () -> Deconstruction.dec(data, -1));
    }

    @Test
    void dec_nullRecord_throws() {
        assertThrows(NullPointerException.class, () -> Deconstruction.dec(null));
    }

    @Test
    void dec_singleFieldRecord_deconstructsToOneValue() {
        var values = Deconstruction.dec(new SingleField("only")).deconstruct();

        assertArrayEquals(new Object[]{"only"}, values);
    }

    @Test
    void deconstruct_unregisteredLocalRecord_throws() {
        record NotMetaGenerated(String a, String b) {}

        var value = new NotMetaGenerated("a", "b");

        assertThrows(IllegalArgumentException.class, () -> Deconstruction.dec(value).deconstruct());
    }
}
