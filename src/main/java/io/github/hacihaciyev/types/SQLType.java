package io.github.hacihaciyev.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public enum SQLType {

    NULL(Void.class),

    TINYINT(byte.class, Byte.class),
    SMALLINT(short.class, Short.class, byte.class, Byte.class),
    INT(int.class, Integer.class, short.class, Short.class, byte.class, Byte.class, AtomicInteger.class),
    INTEGER(int.class, Integer.class, short.class, Short.class, byte.class, Byte.class, AtomicInteger.class),
    BIGINT(long.class, Long.class, int.class, Integer.class, short.class, Short.class, byte.class, Byte.class,
            AtomicLong.class, AtomicInteger.class),

    DECIMAL(BigDecimal.class, BigInteger.class, long.class, Long.class, int.class, Integer.class,
            short.class, Short.class, byte.class, Byte.class),
    NUMERIC(BigDecimal.class, BigInteger.class, long.class, Long.class, int.class, Integer.class,
            short.class, Short.class, byte.class, Byte.class),

    FLOAT(float.class, Float.class, double.class, Double.class, BigDecimal.class),
    REAL(float.class, Float.class, double.class, Double.class, BigDecimal.class),
    DOUBLE(double.class, Double.class, float.class, Float.class, BigDecimal.class),
    DOUBLE_PRECISION(double.class, Double.class),

    MONEY(BigDecimal.class),
    SMALLMONEY(BigDecimal.class),

    BOOLEAN(boolean.class, Boolean.class, AtomicBoolean.class),
    BIT(boolean.class, Boolean.class, AtomicBoolean.class),

    CHAR(char.class, Character.class, CharSequence.class),
    CHARACTER(char.class, Character.class, CharSequence.class),
    NCHAR(char.class, Character.class, CharSequence.class),

    VARCHAR(CharSequence.class),
    CHARACTER_VARYING(CharSequence.class),
    NATIONAL_CHAR(CharSequence.class),
    NVARCHAR(CharSequence.class),
    NATIONAL_CHAR_VARYING(CharSequence.class),

    TEXT(String.class, CharSequence.class),
    CLOB(Clob.class, String.class, StringBuilder.class, CharSequence.class),

    BINARY(byte[].class),
    VARBINARY(byte[].class),
    BINARY_VARYING(byte[].class),
    BLOB(Blob.class, byte[].class),

    DATE(Date.class, LocalDate.class),
    TIME(Time.class, LocalTime.class),
    TIMESTAMP(Timestamp.class, LocalDateTime.class, Instant.class, ZonedDateTime.class, OffsetDateTime.class),
    TIMESTAMP_WITH_TIME_ZONE(OffsetDateTime.class, ZonedDateTime.class, Instant.class),
    TIMESTAMP_WITHOUT_TIME_ZONE(LocalDateTime.class),

    DATETIME(Timestamp.class, LocalDateTime.class),
    DATETIME2(LocalDateTime.class),
    DATETIMEOFFSET(OffsetDateTime.class),
    SMALLDATETIME(Timestamp.class),

    INTERVAL(Duration.class, Period.class),
    YEAR(int.class, Integer.class, Year.class),

    UUID(UUID.class),
    UNIQUEIDENTIFIER(UUID.class),

    XML(CharSequence.class),
    JSON(CharSequence.class),
    JSONB(CharSequence.class),
    JSON_ELEMENT(Object.class),
    GEOMETRY(Object.class),
    GEOGRAPHY(Object.class),
    HIERARCHYID(CharSequence.class),
    ROWVERSION(byte[].class),
    SQL_VARIANT(Object.class),
    CURSOR(Void.class),
    TABLE_TYPE(Void.class),

    ARRAY(Object[].class, List.class),
    LIST(Collection.class, List.class),
    MULTISET(Collection.class),
    SET(Set.class),

    VARIANT(Object.class),
    OBJECT(Object.class),
    ANY(Object.class),

    ENUM(String.class),
    SET_TYPE(Set.class);

    private final Class<?>[] supported;

    SQLType(Class<?>... supported) {
        this.supported = supported;
    }

    public Class<?>[] supportedTypes() {
        return supported;
    }

    public boolean isSupportedType(Class<?> type) {
        if (type == null) return true;
        return Arrays.stream(supportedTypes()).anyMatch(supportedType -> type.isAssignableFrom(supportedType));
    }

    public boolean isSupportedType(Object object) {
        if (object == null) return true;
        return isSupportedType(object.getClass());
    }
}