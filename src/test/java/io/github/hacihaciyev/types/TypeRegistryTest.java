package io.github.hacihaciyev.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static io.github.hacihaciyev.types.TypeRegistry.TypeInfo.None;
import static io.github.hacihaciyev.types.TypeRegistry.TypeInfo.Some;

class TypeRegistryTest {
    
    @Test
    void shouldReturnNoneForNull() {
        assertThat(TypeRegistry.info(null)).isInstanceOf(None.class);
    }

    @Test
    void shouldReturnNoneForUnsupportedType() {
        assertThat(TypeRegistry.info(Object.class)).isInstanceOf(None.class);
    }

    @Test
    void shouldCacheTypeInfo() {
        var first = TypeRegistry.info(String.class);
        var second = TypeRegistry.info(String.class);
        assertThat(first).isSameAs(second);
    }

    @ParameterizedTest
    @MethodSource("allSupportedTypes")
    void shouldRecognizeAsSupported(Class<?> type) {
        assertThat(TypeRegistry.info(type)).isInstanceOf(Some.class);
    }

    static Stream<Class<?>> allSupportedTypes() {
        return Stream.of(
            int.class, long.class, short.class, byte.class,
            double.class, float.class, boolean.class, char.class,

            Integer.class, Long.class, Short.class, Byte.class,
            Double.class, Float.class, Boolean.class, Character.class,

            AtomicInteger.class, AtomicLong.class, AtomicBoolean.class,

            String.class, StringBuilder.class, StringBuffer.class, CharSequence.class,

            BigDecimal.class, BigInteger.class,

            LocalDate.class, LocalTime.class, LocalDateTime.class,
            Instant.class, OffsetDateTime.class, ZonedDateTime.class,
            Duration.class, Period.class, Year.class, YearMonth.class, MonthDay.class,

            Date.class, Time.class, Timestamp.class,

            UUID.class, URI.class, URL.class, Path.class,
            byte[].class, Blob.class, Clob.class, Void.class
        );
    }
    
    @ParameterizedTest
    @MethodSource("setterTestCases")
    void shouldCallCorrectSetterMethod(Object value, SetterAssertion assertion) throws Exception {
        var stmt = mock(PreparedStatement.class);
        var info = (Some) TypeRegistry.info(value.getClass());
        
        info.setter().set(stmt, value, 1);
        
        assertion.verify(stmt);
    }

    static Stream<Arguments> setterTestCases() {
        return Stream.of(
            test(42, stmt -> verify(stmt).setInt(1, 42)),
            test(42L, stmt -> verify(stmt).setLong(1, 42L)),
            test((short) 42, stmt -> verify(stmt).setShort(1, (short) 42)),
            test((byte) 42, stmt -> verify(stmt).setByte(1, (byte) 42)),
            test(42.5, stmt -> verify(stmt).setDouble(1, 42.5)),
            test(42.5f, stmt -> verify(stmt).setFloat(1, 42.5f)),
            test(true, stmt -> verify(stmt).setBoolean(1, true)),
            test('A', stmt -> verify(stmt).setString(1, "A")),
            
            test(new AtomicInteger(99), stmt -> verify(stmt).setInt(1, 99)),
            test(new AtomicLong(99L), stmt -> verify(stmt).setLong(1, 99L)),
            test(new AtomicBoolean(true), stmt -> verify(stmt).setBoolean(1, true)),
            
            test("hello", stmt -> verify(stmt).setString(1, "hello")),
            test(new StringBuilder("test"), stmt -> verify(stmt).setString(1, "test")),
            test(new StringBuffer("test"), stmt -> verify(stmt).setString(1, "test")),
            test(URI.create("https://example.com"), stmt -> verify(stmt).setString(1, "https://example.com")),

            test(new BigDecimal("123.45"), stmt -> verify(stmt).setBigDecimal(1, new BigDecimal("123.45"))),
            test(new BigInteger("12345"), stmt -> verify(stmt).setBigDecimal(1, new BigDecimal(new BigInteger("12345")))),
            
            test(LocalDate.of(2024, 1, 1), stmt -> verify(stmt).setDate(1, Date.valueOf(LocalDate.of(2024, 1, 1)))),
            test(LocalTime.of(12, 30), stmt -> verify(stmt).setTime(1, Time.valueOf(LocalTime.of(12, 30)))),
            test(LocalDateTime.of(2024, 1, 1, 12, 0), stmt -> verify(stmt).setObject(1, LocalDateTime.of(2024, 1, 1, 12, 0))),
            test(Year.of(2024), stmt -> verify(stmt).setInt(1, 2024)),
            test(YearMonth.of(2024, 6), stmt -> verify(stmt).setString(1, "2024-06")),
            
            test(new byte[]{1, 2, 3}, stmt -> verify(stmt).setBytes(1, new byte[]{1, 2, 3})),
            test(UUID.randomUUID(), stmt -> verify(stmt).setObject(eq(1), any(UUID.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("sqlTypeMappings")
    void shouldMapToCorrectSQLTypes(Class<?> type, SQLType... expected) {
        var info = (Some) TypeRegistry.info(type);
        assertThat(info.sqlTypes()).contains(expected);
    }

    @Test
    void shouldHandlePath() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var path = Path.of("/tmp/test");

        var info = (Some) TypeRegistry.info(Path.class);
        info.setter().set(stmt, path, 1);

        verify(stmt).setString(1, path.toString());
    }

    static Stream<Arguments> sqlTypeMappings() {
        return Stream.of(
            mapping(int.class, SQLType.INT, SQLType.INTEGER, SQLType.BIGINT),
            mapping(long.class, SQLType.BIGINT, SQLType.INT, SQLType.INTEGER),
            mapping(String.class, SQLType.VARCHAR, SQLType.TEXT, SQLType.CHAR),
            mapping(boolean.class, SQLType.BOOLEAN, SQLType.BIT),
            mapping(UUID.class, SQLType.UUID, SQLType.UNIQUEIDENTIFIER),
            mapping(LocalDate.class, SQLType.DATE),
            mapping(LocalTime.class, SQLType.TIME),
            mapping(byte[].class, SQLType.BINARY, SQLType.VARBINARY),
            mapping(BigDecimal.class, SQLType.DECIMAL, SQLType.NUMERIC)
        );
    }

    enum TestEnum { ACTIVE, INACTIVE }

    @Test
    void shouldConvertEnumToName() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var info = (Some) TypeRegistry.info(TestEnum.class);
        
        info.setter().set(stmt, TestEnum.ACTIVE, 1);
        
        verify(stmt).setString(1, "ACTIVE");
    }

    record UserId(UUID id) {}
    record UserName(String name) {}
    record InvalidRecord(String a, String b) {}

    @Test
    void shouldUnwrapSingleValueRecord() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var uuid = UUID.randomUUID();
        var userId = new UserId(uuid);
        
        var info = (Some) TypeRegistry.info(UserId.class);
        info.setter().set(stmt, userId, 1);
        
        verify(stmt).setObject(1, uuid);
    }

    @Test
    void shouldInheritSQLTypesFromComponent() {
        var recordInfo = (Some) TypeRegistry.info(UserId.class);
        var uuidInfo = (Some) TypeRegistry.info(UUID.class);
        
        assertThat(recordInfo.sqlTypes()).isEqualTo(uuidInfo.sqlTypes());
    }

    @Test
    void shouldRejectMultiFieldRecord() {
        assertThat(TypeRegistry.info(InvalidRecord.class)).isInstanceOf(None.class);
    }

    @Test
    void shouldRejectNonRecord() {
        class NotARecord {}
        assertThat(TypeRegistry.info(NotARecord.class)).isInstanceOf(None.class);
    }
    
    @FunctionalInterface
    interface SetterAssertion {
        void verify(PreparedStatement stmt) throws Exception;
    }

    static Arguments test(Object value, SetterAssertion assertion) {
        return Arguments.of(value, assertion);
    }

    static Arguments mapping(Class<?> type, SQLType... sqlTypes) {
        return Arguments.of(type, sqlTypes);
    }
}