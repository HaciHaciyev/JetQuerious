package io.github.hacihaciyev.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.PreparedStatement;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParameterSetterValueObjectTest {

    public static class SimpleVO {
        private final String value = "test";
    }

    public static class NullFieldVO {
        private final String value = null;
    }

    public static class InvalidVO {
        private final String field1 = "value1";
        private final String field2 = "value2";
    }

    public static class PrimitiveFieldVO {
        private final int count = 42;
    }

    @BeforeEach
    void clearRegistry() {
        TypeRegistry.FIELDS.clear();
    }

    @Test
    void shouldRegisterAndProcessSimpleVO() throws Exception {
        SimpleVO vo = new SimpleVO();
        PreparedStatement stmt = mock(PreparedStatement.class);
        
        boolean isValid = TypeRegistry.isSupportedType(vo);
        
        assertTrue(isValid);
        assertTrue(TypeRegistry.FIELDS.containsKey(SimpleVO.class));
        
        ParameterSetter.setParameter(stmt, vo, 1);
        verify(stmt).setString(1, "test");
    }

    @Test
    void shouldHandleNullField() throws Exception {
        NullFieldVO vo = new NullFieldVO();
        PreparedStatement stmt = mock(PreparedStatement.class);
        
        TypeRegistry.isSupportedType(vo);
        ParameterSetter.setParameter(stmt, vo, 1);
        
        verify(stmt).setNull(1, Types.NULL);
    }

    @Test
    void shouldRejectInvalidVO() {
        InvalidVO vo = new InvalidVO();
        
        assertFalse(TypeRegistry.isSupportedType(vo));
        assertFalse(TypeRegistry.FIELDS.containsKey(InvalidVO.class));
    }

    @Test
    void shouldHandlePrimitiveField() throws Exception {
        PrimitiveFieldVO vo = new PrimitiveFieldVO();
        PreparedStatement stmt = mock(PreparedStatement.class);
        
        TypeRegistry.isSupportedType(vo);
        ParameterSetter.setParameter(stmt, vo, 1);
        
        verify(stmt).setInt(1, 42);
    }

    @Test
    void shouldThrowForUnregisteredVO() {
        SimpleVO vo = new SimpleVO();
        PreparedStatement stmt = mock(PreparedStatement.class);
        
        assertThrows(IllegalArgumentException.class,
            () -> ParameterSetter.setParameter(stmt, vo, 1));
    }
}