package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.exceptions.InvalidArgumentTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapperTest {

    private ResultSet rs;

    @BeforeEach
    void setUp() {
        rs = mock(ResultSet.class);
    }

    @Test
    void testMapString() throws SQLException {
        when(rs.getString(1)).thenReturn("test");
        String result = Mapper.map(rs, String.class);
        assertEquals("test", result);
    }

    @Test
    void testMapInteger() throws SQLException {
        when(rs.getInt(1)).thenReturn(42);
        Integer result = Mapper.map(rs, Integer.class);
        assertEquals(42, result);
    }

    @Test
    void testMapBoolean() throws SQLException {
        when(rs.getBoolean(1)).thenReturn(true);
        Boolean result = Mapper.map(rs, Boolean.class);
        assertTrue(result);
    }

    @Test
    void testMapUUID() throws SQLException {
        UUID uuid = UUID.randomUUID();
        when(rs.getString(1)).thenReturn(uuid.toString());
        UUID result = Mapper.map(rs, UUID.class);
        assertEquals(uuid, result);
    }

    @Test
    void testMapAtomicTypes() throws SQLException {
        when(rs.getInt(1)).thenReturn(10);
        AtomicInteger ai = Mapper.map(rs, AtomicInteger.class);
        assertEquals(10, ai.get());

        when(rs.getLong(1)).thenReturn(20L);
        AtomicLong al = Mapper.map(rs, AtomicLong.class);
        assertEquals(20L, al.get());

        when(rs.getBoolean(1)).thenReturn(true);
        AtomicBoolean ab = Mapper.map(rs, AtomicBoolean.class);
        assertTrue(ab.get());
    }

    @Test
    void testUnsupportedTypeThrows() {
        assertThrows(InvalidArgumentTypeException.class, () -> Mapper.map(rs, Object.class));
    }

    @Test
    void testSQLExceptionThrows() throws SQLException {
        when(rs.getString(1)).thenThrow(new SQLException("fail"));
        assertThrows(InvalidArgumentTypeException.class, () -> Mapper.map(rs, String.class));
    }

    @Test
    void testNullUUIDReturnsNull() throws SQLException {
        when(rs.getString(1)).thenReturn(null);
        UUID result = Mapper.map(rs, UUID.class);
        assertNull(result);
    }

    @Test
    void testNullStringBuilderReturnsNull() throws SQLException {
        when(rs.getString(1)).thenReturn(null);
        assertNull(Mapper.map(rs, StringBuilder.class));
    }

}
