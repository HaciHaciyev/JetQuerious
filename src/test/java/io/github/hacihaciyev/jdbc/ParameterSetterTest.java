package io.github.hacihaciyev.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParameterSetterTest {

    @Test
    void setParameter_withNull_shouldSetNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, null, 1);
        verify(stmt).setNull(1, Types.NULL);
    }

    @Test
    void setParameter_withString_shouldSetString() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, "test string", 1);
        verify(stmt).setString(1, "test string");
    }

    @Test
    void setParameter_withStringBuilder_shouldSetString() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, new StringBuilder("string builder"), 1);
        verify(stmt).setString(1, "string builder");
    }

    @Test
    void setParameter_withStringBuffer_shouldSetString() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, new StringBuffer("string buffer"), 1);
        verify(stmt).setString(1, "string buffer");
    }

    @Test
    void setParameter_withCharacter_shouldSetString() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, 'c', 1);
        verify(stmt).setString(1, "c");
    }

    @Test
    void setParameter_withInteger_shouldSetInt() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, 123, 1);
        verify(stmt).setInt(1, 123);
    }

    @Test
    void setParameter_withLong_shouldSetLong() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, 123L, 1);
        verify(stmt).setLong(1, 123L);
    }

    @Test
    void setParameter_withDouble_shouldSetDouble() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, 123.45, 1);
        verify(stmt).setDouble(1, 123.45);
    }

    @Test
    void setParameter_withFloat_shouldSetFloat() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, 123.45f, 1);
        verify(stmt).setFloat(1, 123.45f);
    }

    @Test
    void setParameter_withBigDecimal_shouldSetBigDecimal() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        BigDecimal value = BigDecimal.valueOf(123.45);
        ParameterSetter.setParameter(stmt, value, 1);
        verify(stmt).setBigDecimal(1, value);
    }

    @Test
    void setParameter_withBigInteger_shouldSetBigDecimal() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        BigInteger value = BigInteger.valueOf(123);
        ParameterSetter.setParameter(stmt, value, 1);
        verify(stmt).setBigDecimal(1, new BigDecimal(value));
    }

    @Test
    void setParameter_withBoolean_shouldSetBoolean() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, true, 1);
        verify(stmt).setBoolean(1, true);
    }

    @Test
    void setParameter_withAtomicBoolean_shouldSetBoolean() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, new AtomicBoolean(true), 1);
        verify(stmt).setBoolean(1, true);
    }

    @Test
    void setParameter_withUUID_shouldSetString() throws SQLException {
        UUID uuid = UUID.randomUUID();
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, uuid, 1);
        verify(stmt).setString(1, uuid.toString());
    }

    @Test
    void setParameter_withByteArray_shouldSetBytes() throws SQLException {
        byte[] bytes = new byte[]{1, 2, 3};
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, bytes, 1);
        verify(stmt).setBytes(1, bytes);
    }

    @Test
    void setParameter_withLocalDateTime_shouldSetTimestamp() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, now, 1);
        verify(stmt).setObject(1, Timestamp.valueOf(now));
    }

    @Test
    void setParameter_withLocalDate_shouldSetDate() throws SQLException {
        LocalDate now = LocalDate.now();
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, now, 1);
        verify(stmt).setObject(1, java.sql.Date.valueOf(now));
    }

    @Test
    void setParameter_withInstant_shouldSetTimestamp() throws SQLException {
        Instant now = Instant.now();
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, now, 1);
        verify(stmt).setObject(1, Timestamp.from(now));
    }

    @Test
    void setParameter_withYear_shouldSetInt() throws SQLException {
        Year year = Year.of(2023);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, year, 1);
        verify(stmt).setInt(1, 2023);
    }

    @Test
    void setParameter_withURI_shouldSetString() throws SQLException {
        URI uri = URI.create("http://example.com");
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, uri, 1);
        verify(stmt).setString(1, uri.toString());
    }

    @Test
    void setParameter_withURL_shouldSetURL() throws Exception {
        URL url = new URL("http://example.com");
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, url, 1);
        verify(stmt).setURL(1, url);
    }

    @Test
    void setParameter_withEnum_shouldSetString() throws SQLException {
        enum TestEnum { VALUE1, VALUE2 }
        
        PreparedStatement stmt = mock(PreparedStatement.class);
        ParameterSetter.setParameter(stmt, TestEnum.VALUE1, 1);
        verify(stmt).setString(1, "VALUE1");
    }

    @Test
    void setParameter_withUnsupportedType_shouldThrowIllegalArgumentException() {
        class UnsupportedType {}
        
        PreparedStatement stmt = mock(PreparedStatement.class);
        assertThrows(IllegalArgumentException.class, 
            () -> ParameterSetter.setParameter(stmt, new UnsupportedType(), 1));
    }

    @Test
    void setParameter_withSQLException_shouldPropagateException() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        doThrow(new SQLException("Test exception")).when(stmt).setString(anyInt(), anyString());
        
        assertThrows(SQLException.class,
            () -> ParameterSetter.setParameter(stmt, "test", 1));
    }
}