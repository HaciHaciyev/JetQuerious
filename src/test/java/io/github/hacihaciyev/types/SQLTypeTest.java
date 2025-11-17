package io.github.hacihaciyev.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SQLType.isSupportedType() Tests")
class SQLTypeTest {

    @Test
    @DisplayName("NULL: should accept null object")
    void testNull_AcceptsNullObject() {
        assertTrue(SQLType.NULL.isSupportedType((Object) null));
    }

    @Test
    @DisplayName("NULL: should accept null class")
    void testNull_AcceptsNullClass() {
        assertTrue(SQLType.NULL.isSupportedType(null));
    }

    @Test
    @DisplayName("NULL: should accept Void.class")
    void testNull_AcceptsVoidClass() {
        assertTrue(SQLType.NULL.isSupportedType(Void.class));
    }
    
    @ParameterizedTest
    @MethodSource("integerTypeProvider")
    @DisplayName("INT: should accept all integer compatible types")
    void testInt_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.INT.isSupportedType(value));
    }

    static Stream<Arguments> integerTypeProvider() {
        return Stream.of(
            Arguments.of(42),
            Arguments.of((byte) 1),
            Arguments.of((short) 100),
            Arguments.of(500),
            Arguments.of((byte) 5),
            Arguments.of((short) 50),
            Arguments.of(new AtomicInteger(10))
        );
    }

    @Test
    @DisplayName("INT: should reject incompatible types")
    void testInt_RejectsIncompatibleTypes() {
        assertFalse(SQLType.INT.isSupportedType("string"));
        assertFalse(SQLType.INT.isSupportedType(3.14));
        assertFalse(SQLType.INT.isSupportedType(true));
        assertFalse(SQLType.INT.isSupportedType(new Object()));
    }

    @ParameterizedTest
    @MethodSource("bigintTypeProvider")
    @DisplayName("BIGINT: should accept all long compatible types")
    void testBigint_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.BIGINT.isSupportedType(value));
    }

    static Stream<Arguments> bigintTypeProvider() {
        return Stream.of(
            Arguments.of(1234567890123456789L),
            Arguments.of(999L),
            Arguments.of(42),
            Arguments.of((short) 100),
            Arguments.of((byte) 1),
            Arguments.of(new AtomicLong(500)),
            Arguments.of(new AtomicInteger(250))
        );
    }

    @ParameterizedTest
    @MethodSource("decimalTypeProvider")
    @DisplayName("DECIMAL: should accept all decimal compatible types")
    void testDecimal_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.DECIMAL.isSupportedType(value));
    }

    static Stream<Arguments> decimalTypeProvider() {
        return Stream.of(
            Arguments.of(new BigDecimal("123.45")),
            Arguments.of(new BigInteger("999999999999")),
            Arguments.of(1234567890123456789L),
            Arguments.of(42),
            Arguments.of((short) 100),
            Arguments.of((byte) 1)
        );
    }

    @ParameterizedTest
    @MethodSource("floatTypeProvider")
    @DisplayName("FLOAT: should accept all float compatible types")
    void testFloat_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.FLOAT.isSupportedType(value));
    }

    static Stream<Arguments> floatTypeProvider() {
        return Stream.of(
            Arguments.of(3.14f),
            Arguments.of(2.71f),
            Arguments.of(3.14159),
            Arguments.of(2.71828),
            Arguments.of(new BigDecimal("99.99"))
        );
    }

    @Test
    @DisplayName("DOUBLE: should accept double and float types")
    void testDouble_AcceptsSupportedTypes() {
        assertTrue(SQLType.DOUBLE.isSupportedType(3.14159));
        assertTrue(SQLType.DOUBLE.isSupportedType(2.71828));
        assertTrue(SQLType.DOUBLE.isSupportedType(3.14f));
        assertTrue(SQLType.DOUBLE.isSupportedType(1.5f));
        assertTrue(SQLType.DOUBLE.isSupportedType(new BigDecimal("123.456")));
    }
    
    @ParameterizedTest
    @MethodSource("booleanTypeProvider")
    @DisplayName("BOOLEAN: should accept all boolean compatible types")
    void testBoolean_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.BOOLEAN.isSupportedType(value));
    }

    static Stream<Arguments> booleanTypeProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false),
            Arguments.of(Boolean.TRUE),
            Arguments.of(Boolean.FALSE),
            Arguments.of(new AtomicBoolean(true))
        );
    }

    @Test
    @DisplayName("BIT: should be equivalent to BOOLEAN")
    void testBit_AcceptsSameTypesAsBoolean() {
        assertTrue(SQLType.BIT.isSupportedType(true));
        assertTrue(SQLType.BIT.isSupportedType(Boolean.FALSE));
        assertTrue(SQLType.BIT.isSupportedType(new AtomicBoolean(false)));
    }

    @ParameterizedTest
    @MethodSource("charTypeProvider")
    @DisplayName("CHAR: should accept character and CharSequence types")
    void testChar_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.CHAR.isSupportedType(value));
    }

    static Stream<Arguments> charTypeProvider() {
        return Stream.of(
            Arguments.of('A'),
            Arguments.of('Z'),
            Arguments.of("Hello"),
            Arguments.of(new StringBuilder("World")),
            Arguments.of(new StringBuffer("Buffer"))
        );
    }

    @ParameterizedTest
    @MethodSource("varcharTypeProvider")
    @DisplayName("VARCHAR: should accept CharSequence types")
    void testVarchar_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.VARCHAR.isSupportedType(value));
    }

    static Stream<Arguments> varcharTypeProvider() {
        return Stream.of(
            Arguments.of("String"),
            Arguments.of(new StringBuilder("Builder")),
            Arguments.of(new StringBuffer("Buffer"))
        );
    }

    @Test
    @DisplayName("TEXT: should accept String and CharSequence")
    void testText_AcceptsSupportedTypes() {
        assertTrue(SQLType.TEXT.isSupportedType("Hello World"));
        assertTrue(SQLType.TEXT.isSupportedType(new StringBuilder("Test")));
        assertTrue(SQLType.TEXT.isSupportedType(String.class));
    }

    @Test
    @DisplayName("BINARY: should accept byte arrays")
    void testBinary_AcceptsByteArray() {
        assertTrue(SQLType.BINARY.isSupportedType(new byte[]{1, 2, 3}));
        assertTrue(SQLType.BINARY.isSupportedType(new byte[0]));
        assertTrue(SQLType.BINARY.isSupportedType(byte[].class));
    }

    @Test
    @DisplayName("BINARY: should reject non-byte arrays")
    void testBinary_RejectsNonByteArray() {
        assertFalse(SQLType.BINARY.isSupportedType(new int[]{1, 2, 3}));
        assertFalse(SQLType.BINARY.isSupportedType("not a byte array"));
    }

    @Test
    @DisplayName("VARBINARY: should accept byte arrays")
    void testVarbinary_AcceptsByteArray() {
        assertTrue(SQLType.VARBINARY.isSupportedType(new byte[]{10, 20, 30}));
    }

    @ParameterizedTest
    @MethodSource("dateTypeProvider")
    @DisplayName("DATE: should accept Date and LocalDate")
    void testDate_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.DATE.isSupportedType(value));
    }

    static Stream<Arguments> dateTypeProvider() {
        return Stream.of(
            Arguments.of(Date.valueOf(LocalDate.now())),
            Arguments.of(LocalDate.now())
        );
    }

    @ParameterizedTest
    @MethodSource("timeTypeProvider")
    @DisplayName("TIME: should accept Time and LocalTime")
    void testTime_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.TIME.isSupportedType(value));
    }

    static Stream<Arguments> timeTypeProvider() {
        return Stream.of(
            Arguments.of(new Time(System.currentTimeMillis())),
            Arguments.of(LocalTime.now())
        );
    }

    @ParameterizedTest
    @MethodSource("timestampTypeProvider")
    @DisplayName("TIMESTAMP: should accept all timestamp types")
    void testTimestamp_AcceptsSupportedTypes(Object value) {
        assertTrue(SQLType.TIMESTAMP.isSupportedType(value));
    }

    static Stream<Arguments> timestampTypeProvider() {
        return Stream.of(
            Arguments.of(new Timestamp(System.currentTimeMillis())),
            Arguments.of(LocalDateTime.now()),
            Arguments.of(Instant.now()),
            Arguments.of(ZonedDateTime.now()),
            Arguments.of(OffsetDateTime.now())
        );
    }

    @Test
    @DisplayName("TIMESTAMP_WITH_TIME_ZONE: should accept timezone-aware types")
    void testTimestampWithTimeZone_AcceptsSupportedTypes() {
        assertTrue(SQLType.TIMESTAMP_WITH_TIME_ZONE.isSupportedType(OffsetDateTime.now()));
        assertTrue(SQLType.TIMESTAMP_WITH_TIME_ZONE.isSupportedType(ZonedDateTime.now()));
        assertTrue(SQLType.TIMESTAMP_WITH_TIME_ZONE.isSupportedType(Instant.now()));
    }

    @Test
    @DisplayName("INTERVAL: should accept Duration and Period")
    void testInterval_AcceptsSupportedTypes() {
        assertTrue(SQLType.INTERVAL.isSupportedType(Duration.ofHours(2)));
        assertTrue(SQLType.INTERVAL.isSupportedType(Period.ofDays(7)));
    }
    
    @Test
    @DisplayName("UUID: should accept UUID objects")
    void testUuid_AcceptsUuidType() {
        assertTrue(SQLType.UUID.isSupportedType(UUID.randomUUID()));
        assertTrue(SQLType.UUID.isSupportedType(UUID.class));
    }

    @Test
    @DisplayName("UNIQUEIDENTIFIER: should accept UUID objects")
    void testUniqueIdentifier_AcceptsUuidType() {
        assertTrue(SQLType.UNIQUEIDENTIFIER.isSupportedType(UUID.randomUUID()));
    }

    @Test
    @DisplayName("ARRAY: should accept arrays and Lists")
    void testArray_AcceptsSupportedTypes() {
        assertTrue(SQLType.ARRAY.isSupportedType(new Object[]{}));
        assertTrue(SQLType.ARRAY.isSupportedType(new String[]{"a", "b"}));
        assertTrue(SQLType.ARRAY.isSupportedType(new ArrayList<>()));
        assertTrue(SQLType.ARRAY.isSupportedType(List.of(1, 2, 3)));
    }

    @Test
    @DisplayName("LIST: should accept List and Collection types")
    void testList_AcceptsSupportedTypes() {
        assertTrue(SQLType.LIST.isSupportedType(new ArrayList<>()));
        assertTrue(SQLType.LIST.isSupportedType(new LinkedList<>()));
        assertTrue(SQLType.LIST.isSupportedType(List.of(1, 2, 3)));
    }

    @Test
    @DisplayName("SET: should accept Set types")
    void testSet_AcceptsSupportedTypes() {
        assertTrue(SQLType.SET.isSupportedType(new HashSet<>()));
        assertTrue(SQLType.SET.isSupportedType(new TreeSet<>()));
        assertTrue(SQLType.SET.isSupportedType(Set.of(1, 2, 3)));
    }
    
    @Test
    @DisplayName("XML/JSON: should accept CharSequence types")
    void testXmlJson_AcceptCharSequence() {
        assertTrue(SQLType.XML.isSupportedType("<?xml version='1.0'?>"));
        assertTrue(SQLType.JSON.isSupportedType("{\"key\":\"value\"}"));
        assertTrue(SQLType.JSONB.isSupportedType("[1,2,3]"));
    }

    @Test
    @DisplayName("VARIANT/OBJECT/ANY: should accept any object")
    void testVariantTypes_AcceptAnyObject() {
        Object[] testObjects = {
            "string", 42, 3.14, true, new Object(), 
            new ArrayList<>(), new HashMap<>(), UUID.randomUUID()
        };
        
        for (Object obj : testObjects) {
            assertTrue(SQLType.VARIANT.isSupportedType(obj));
            assertTrue(SQLType.OBJECT.isSupportedType(obj));
            assertTrue(SQLType.ANY.isSupportedType(obj));
        }
    }

    @Test
    @DisplayName("All types should accept null object")
    void testAllTypes_AcceptNullObject() {
        for (SQLType type : SQLType.values()) {
            assertTrue(type.isSupportedType((Object) null), 
                "SQLType." + type + " should accept null object");
        }
    }

    @Test
    @DisplayName("All types should accept null class")
    void testAllTypes_AcceptNullClass() {
        for (SQLType type : SQLType.values()) {
            assertTrue(type.isSupportedType(null),
                "SQLType." + type + " should accept null class");
        }
    }

    @Test
    @DisplayName("Should reject incompatible primitive types")
    void testRejectIncompatiblePrimitives() {
        assertFalse(SQLType.BOOLEAN.isSupportedType(42));
        assertFalse(SQLType.INT.isSupportedType(true));
        assertFalse(SQLType.VARCHAR.isSupportedType(123));
    }

    @Test
    @DisplayName("Should handle inheritance correctly")
    void testInheritanceHandling() {
        assertTrue(SQLType.VARCHAR.isSupportedType(new StringBuilder()));
        
        assertTrue(SQLType.LIST.isSupportedType(new ArrayList<>()));
        
        assertTrue(SQLType.SET.isSupportedType(new HashSet<>()));
    }

    @Test
    @DisplayName("Should handle wrapper classes correctly")
    void testWrapperClasses() {
        assertTrue(SQLType.INT.isSupportedType(Integer.class));
        assertTrue(SQLType.BIGINT.isSupportedType(Long.class));
        assertTrue(SQLType.BOOLEAN.isSupportedType(Boolean.class));
        assertTrue(SQLType.DOUBLE.isSupportedType(Double.class));
    }

    @Test
    @DisplayName("Should handle repeated calls efficiently")
    void testPerformance() {
        Object testObject = 42;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 10000; i++) {
            SQLType.INT.isSupportedType(testObject);
        }
        
        long duration = System.nanoTime() - startTime;
        assertTrue(duration < 100_000_000,
            "10000 calls should complete in under 100ms");
    }
}