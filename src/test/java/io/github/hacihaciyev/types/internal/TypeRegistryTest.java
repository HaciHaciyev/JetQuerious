package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.types.AsObject;
import io.github.hacihaciyev.types.AsString;
import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.types.UUIDStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static io.github.hacihaciyev.types.internal.TypeInfo.None;
import static io.github.hacihaciyev.types.internal.TypeInfo.Some;
import static io.github.hacihaciyev.types.internal.TypeInfo.WithFactory;

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
        var first  = TypeRegistry.info(String.class);
        var second = TypeRegistry.info(String.class);
        assertThat(first).isSameAs(second);
    }

    @ParameterizedTest
    @MethodSource("allSupportedTypes")
    void shouldRecognizeAsSupported(Class<?> type) {
        assertThat(TypeRegistry.info(type)).isInstanceOf(Some.class);
    }

    @ParameterizedTest
    @MethodSource("allSupportedTypes")
    void shouldHaveNonNullGetter(Class<?> type) {
        var info = (Some) TypeRegistry.info(type);
        assertThat(info.getter()).isNotNull();
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
    @MethodSource("getterTestCases")
    void shouldCallCorrectGetterMethod(Class<?> type, GetterStub stub, Object expected) throws Exception {
        var rs   = mock(ResultSet.class);
        stub.stub(rs);
        var info = (Some) TypeRegistry.info(type);

        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> getterTestCases() {
        return Stream.of(
            getterTest(int.class,        rs -> when(rs.getInt("col")).thenReturn(42), 42),
            getterTest(Integer.class,    rs -> when(rs.getInt("col")).thenReturn(42), 42),
            getterTest(long.class,       rs -> when(rs.getLong("col")).thenReturn(42L), 42L),
            getterTest(Long.class,       rs -> when(rs.getLong("col")).thenReturn(42L), 42L),
            getterTest(short.class,      rs -> when(rs.getShort("col")).thenReturn((short) 7), (short) 7),
            getterTest(byte.class,       rs -> when(rs.getByte("col")).thenReturn((byte) 3), (byte) 3),
            getterTest(double.class,     rs -> when(rs.getDouble("col")).thenReturn(1.5), 1.5),
            getterTest(float.class,      rs -> when(rs.getFloat("col")).thenReturn(1.5f), 1.5f),
            getterTest(boolean.class,    rs -> when(rs.getBoolean("col")).thenReturn(true), true),
            getterTest(Boolean.class,    rs -> when(rs.getBoolean("col")).thenReturn(true), true),

            getterTest(String.class, rs -> when(rs.getString("col")).thenReturn("hello"), "hello"),

            getterTest(BigDecimal.class,
                rs -> when(rs.getBigDecimal("col")).thenReturn(new BigDecimal("123.45")),
                new BigDecimal("123.45")),

            getterTest(BigInteger.class,
                rs -> when(rs.getBigDecimal("col")).thenReturn(new BigDecimal("12345")),
                new BigInteger("12345")),

            getterTest(byte[].class,
                rs -> when(rs.getBytes("col")).thenReturn(new byte[]{1, 2, 3}),
                new byte[]{1, 2, 3})
        );
    }

    @Test
    void shouldReadUUIDViaGetter() throws Exception {
        var rs   = mock(ResultSet.class);
        var uuid = UUID.randomUUID();
        when(rs.getObject("col", UUID.class)).thenReturn(uuid);

        var info = (Some) TypeRegistry.info(UUID.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(uuid);
    }

    @Test
    void shouldReadLocalDateViaGetter() throws Exception {
        var rs   = mock(ResultSet.class);
        var date = LocalDate.of(2024, 1, 1);
        when(rs.getObject("col", LocalDate.class)).thenReturn(date);

        var info = (Some) TypeRegistry.info(LocalDate.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(date);
    }

    @Test
    void shouldReadLocalDateTimeViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        var dt = LocalDateTime.of(2024, 1, 1, 12, 0);
        when(rs.getObject("col", LocalDateTime.class)).thenReturn(dt);

        var info = (Some) TypeRegistry.info(LocalDateTime.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(dt);
    }

    @Test
    void shouldReadInstantViaGetter_derivedFromOffsetDateTime() throws Exception {
        var rs  = mock(ResultSet.class);
        var odt = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        when(rs.getObject("col", OffsetDateTime.class)).thenReturn(odt);

        var info = (Some) TypeRegistry.info(Instant.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(odt.toInstant());
    }

    @Test
    void shouldReadZonedDateTimeViaGetter_derivedFromOffsetDateTime() throws Exception {
        var rs  = mock(ResultSet.class);
        var odt = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        when(rs.getObject("col", OffsetDateTime.class)).thenReturn(odt);

        var info = (Some) TypeRegistry.info(ZonedDateTime.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(odt.toZonedDateTime());
    }

    @Test
    void shouldReadYearViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getInt("col")).thenReturn(2024);

        var info = (Some) TypeRegistry.info(Year.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(Year.of(2024));
    }

    @Test
    void shouldReadYearMonthViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("2024-06");

        var info = (Some) TypeRegistry.info(YearMonth.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(YearMonth.of(2024, 6));
    }

    @Test
    void shouldReadURIViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("https://example.com");

        var info = (Some) TypeRegistry.info(URI.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(URI.create("https://example.com"));
    }

    @Test
    void shouldReadPathViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("/tmp/test");

        var info = (Some) TypeRegistry.info(Path.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(Path.of("/tmp/test"));
    }

    @Test
    void shouldReadAtomicIntegerViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getInt("col")).thenReturn(7);

        var info = (Some) TypeRegistry.info(AtomicInteger.class);
        var result = (AtomicInteger) info.getter().get(rs, "col");

        assertThat(result.get()).isEqualTo(7);
    }

    @Test
    void shouldReadCharacterViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("A");

        var info = (Some) TypeRegistry.info(Character.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo('A');
    }

    @Test
    void shouldReadCharacterViaGetter_emptyString_returnsNull() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("");

        var info = (Some) TypeRegistry.info(Character.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isNull();
    }

    @Test
    void shouldReadVoidViaGetter_alwaysNull() throws Exception {
        var rs   = mock(ResultSet.class);
        var info = (Some) TypeRegistry.info(Void.class);

        var result = info.getter().get(rs, "col");

        assertThat(result).isNull();
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

    @Test
    void shouldReadEnumNameAsStringViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("ACTIVE");

        var info = (Some) TypeRegistry.info(TestEnum.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo("ACTIVE");
    }

    @Test
    void shouldHandleZonedDateTimeWithBakuZone() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var zdt = ZonedDateTime.of(
                2024, 1, 1, 12, 0, 0, 0,
                ZoneId.of("Asia/Baku")
        );

        var info = (Some) TypeRegistry.info(ZonedDateTime.class);
        info.setter().set(stmt, zdt, 1);

        verify(stmt).setObject(
                eq(1),
                eq(zdt.toOffsetDateTime()),
                eq(JDBCType.TIMESTAMP_WITH_TIMEZONE)
        );
    }

    @Test
    void shouldHandleUUIDStrategyNative() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var uuid = UUID.randomUUID();
        var strategy = new UUIDStrategy.Native(uuid);

        var info = (Some) TypeRegistry.info(strategy.getClass());
        info.setter().set(stmt, strategy, 1);

        verify(stmt).setObject(1, uuid);
    }

    @Test
    void shouldHandleUUIDStrategyCharseq() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var uuid = UUID.randomUUID();
        var strategy = new UUIDStrategy.Charseq(uuid);

        var info = (Some) TypeRegistry.info(strategy.getClass());
        info.setter().set(stmt, strategy, 1);

        verify(stmt).setString(1, uuid.toString());
    }

    @Test
    void shouldHandleUUIDStrategyBinary() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var uuid = UUID.randomUUID();
        var strategy = new UUIDStrategy.Binary(uuid);

        var info = (Some) TypeRegistry.info(strategy.getClass());
        info.setter().set(stmt, strategy, 1);

        verify(stmt).setBytes(eq(1), argThat(bytes -> bytes.length == 16));
    }

    @Test
    void shouldHandleAsObject() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var value = 123;
        var asObject = new AsObject(value);

        var info = (Some) TypeRegistry.info(AsObject.class);
        info.setter().set(stmt, asObject, 1);

        verify(stmt).setObject(1, value);
    }

    @Test
    void shouldReadAsObjectViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getObject("col")).thenReturn(123);

        var info = (Some) TypeRegistry.info(AsObject.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(123);
    }

    @Test
    void shouldHandleAsString() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var asString = new AsString(42);

        var info = (Some) TypeRegistry.info(AsString.class);
        info.setter().set(stmt, asString, 1);

        verify(stmt).setString(1, "42");
    }

    @Test
    void shouldReadAsStringViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("42");

        var info = (Some) TypeRegistry.info(AsString.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo("42");
    }

    public record UserId(UUID id) {}
    public record UserName(String name) {}
    public record InvalidRecord(String a, String b) {}

    @Test
    void shouldUnwrapSingleValueRecord() throws Exception {
        var stmt = mock(PreparedStatement.class);
        var uuid = UUID.randomUUID();
        var userId = new UserId(uuid);

        var info = (TypeInfoOk) TypeRegistry.info(UserId.class);
        info.setter().set(stmt, userId, 1);

        verify(stmt).setObject(1, uuid);
    }

    @Test
    void shouldConstructSingleValueRecordViaGetter() throws Exception {
        var rs   = mock(ResultSet.class);
        var uuid = UUID.randomUUID();
        when(rs.getObject("col", UUID.class)).thenReturn(uuid);

        var info = (TypeInfoOk) TypeRegistry.info(UserId.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(new UserId(uuid));
    }

    @Test
    void shouldConstructUserNameViaGetter() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("Alice");

        var info = (TypeInfoOk) TypeRegistry.info(UserName.class);
        var result = info.getter().get(rs, "col");

        assertThat(result).isEqualTo(new UserName("Alice"));
    }

    @Test
    void shouldInheritSQLTypesFromComponent() {
        var recordInfo = (TypeInfoOk) TypeRegistry.info(UserId.class);
        var uuidInfo   = (TypeInfoOk) TypeRegistry.info(UUID.class);

        assertThat(recordInfo.sqlTypes()).isEqualTo(uuidInfo.sqlTypes());
    }

    @Test
    void shouldSuccessfullyUseFactory() {
        var userId     = new UserId(UUID.randomUUID());
        var userIdInfo = (WithFactory<UserId>) TypeRegistry.info(UserId.class);

        assertDoesNotThrow(() -> userIdInfo.factory().create(userIdInfo.objects(userId)));
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

    @FunctionalInterface
    interface GetterStub {
        void stub(ResultSet rs) throws Exception;
    }

    static Arguments test(Object value, SetterAssertion assertion) {
        return Arguments.of(value, assertion);
    }

    static Arguments mapping(Class<?> type, SQLType... sqlTypes) {
        return Arguments.of(type, sqlTypes);
    }

    static Arguments getterTest(Class<?> type, GetterStub stub, Object expected) {
        return Arguments.of(type, stub, expected);
    }
}