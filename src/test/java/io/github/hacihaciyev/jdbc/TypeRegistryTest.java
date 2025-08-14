package io.github.hacihaciyev.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TypeRegistryTest {

    @Test
    void testValidateArrayDefinitionSupported() {
        assertDoesNotThrow(() -> TypeRegistry.validateArrayDefinition("text"));
        assertDoesNotThrow(() -> TypeRegistry.validateArrayDefinition("INTEGER"));
        assertDoesNotThrow(() -> TypeRegistry.validateArrayDefinition("uuid"));
    }

    @Test
    void testValidateArrayDefinitionUnsupported() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TypeRegistry.validateArrayDefinition("unsupportedType"));
        assertTrue(ex.getMessage().contains("Unsupported array definition"));
    }

    @Test
    void testValidateArrayElementsMatchDefinitionValid() {
        String[] arr = {"a", "b", "c"};
        assertDoesNotThrow(() -> TypeRegistry.validateArrayElementsMatchDefinition(arr, "text"));

        Integer[] nums = {1, 2, 3};
        assertDoesNotThrow(() -> TypeRegistry.validateArrayElementsMatchDefinition(nums, "integer"));

        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID()};
        assertDoesNotThrow(() -> TypeRegistry.validateArrayElementsMatchDefinition(uuids, "uuid"));

        Object[] mixedWithNulls = {null, 1, 2};
        assertDoesNotThrow(() -> TypeRegistry.validateArrayElementsMatchDefinition(mixedWithNulls, "int"));
    }

    @Test
    void testValidateArrayElementsMatchDefinitionInvalid() {
        Object[] arr = {1, "string"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TypeRegistry.validateArrayElementsMatchDefinition(arr, "int"));
        assertTrue(ex.getMessage().contains("does not match expected type"));
    }

    @Test
    void testValidateArrayElementsMatchDefinitionUnsupportedType() {
        Object[] arr = {1, 2};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TypeRegistry.validateArrayElementsMatchDefinition(arr, "unsupported"));
        assertTrue(ex.getMessage().contains("Cannot determine expected type"));
    }

    static class VOInt {
        private final Integer value;

        VOInt(Integer value) { this.value = value; }
    }

    static class VOUnsupported {
        private final Object value;
        VOUnsupported(Object value) { this.value = value; }
    }

    enum SampleEnum { A, B }

    @Test
    void testIsSupportedTypePrimitiveAndWrapper() {
        assertTrue(TypeRegistry.isSupportedType(1));
        assertTrue(TypeRegistry.isSupportedType("test"));
        assertTrue(TypeRegistry.isSupportedType(true));
        assertTrue(TypeRegistry.isSupportedType(null));
    }

    @Test
    void testIsSupportedTypeEnum() {
        assertTrue(TypeRegistry.isSupportedType(SampleEnum.A));
    }

    @Test
    void testIsSupportedTypeValueObjectSupported() {
        VOInt vo = new VOInt(42);
        assertTrue(TypeRegistry.isSupportedType(vo));

        Field field = TypeRegistry.FIELDS.get(VOInt.class);
        assertNotNull(field);
        assertEquals("value", field.getName());
    }

    @Test
    void testIsSupportedTypeValueObjectUnsupported() {
        VOUnsupported vo = new VOUnsupported(new Object());
        assertFalse(TypeRegistry.isSupportedType(vo));
    }

    @Test
    void testIsSupportedTypeValueObjectNullField() {
        VOInt vo = new VOInt(null);
        assertTrue(TypeRegistry.isSupportedType(vo));
    }
}
