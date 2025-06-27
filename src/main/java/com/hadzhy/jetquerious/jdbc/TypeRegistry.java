package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.InvalidArgumentTypeException;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class TypeRegistry {



    static final Map<Class<?>, Field> FIELDS = new ConcurrentHashMap<>();

    static final Map<String, Class<?>> SUPPORTED_ARRAY_TYPES = Map.ofEntries(
            Map.entry("text", String.class),
            Map.entry("varchar", String.class),
            Map.entry("int", Integer.class),
            Map.entry("integer", Integer.class),
            Map.entry("bigint", Long.class),
            Map.entry("boolean", Boolean.class),
            Map.entry("uuid", java.util.UUID.class),
            Map.entry("date", java.sql.Date.class),
            Map.entry("timestamp", java.sql.Timestamp.class)
    );

    static final Map<Class<?>, Function<ResultSet, ?>> TYPES_MAPPER = typesMapper();

    private TypeRegistry() {}

    static <T> T map(ResultSet rs, Class<T> type) {
        Function<ResultSet, ?> resultSetFunction = TYPES_MAPPER.get(type);
        if (resultSetFunction == null) throw new InvalidArgumentTypeException("Unsupported wrapper type.");
        return (T) resultSetFunction.apply(rs);
    }

    static void setParameter(PreparedStatement statement, Object param, int i) throws SQLException {
        switch (param) {
            case UUID uuid -> statement.setObject(i + 1, uuid.toString());
            case Time time -> statement.setTime(i + 1, time);
            case Timestamp timestamp -> statement.setTimestamp(i + 1, timestamp);
            case LocalDateTime localDateTime -> statement.setObject(i + 1, Timestamp.valueOf(localDateTime));
            case LocalDate localDate -> statement.setObject(i + 1, java.sql.Date.valueOf(localDate));
            case LocalTime localTime -> statement.setObject(i + 1, Time.valueOf(localTime));
            case Instant instant -> statement.setObject(i + 1, Timestamp.from(instant));
            case ZonedDateTime zonedDateTime -> statement.setObject(i + 1, Timestamp.from(zonedDateTime.toInstant()));
            case OffsetDateTime offsetDateTime -> statement.setObject(i + 1, Timestamp.from(offsetDateTime.toInstant()));
            case Duration duration -> statement.setObject(i + 1, duration);
            case Period period -> statement.setObject(i + 1, period);
            case Year year -> statement.setInt(i + 1, year.getValue());
            case YearMonth yearMonth -> statement.setString(i + 1, yearMonth.toString());
            case MonthDay monthDay -> statement.setString(i + 1, monthDay.toString());
            case BigDecimal bigDecimal -> statement.setBigDecimal(i + 1, bigDecimal);
            case BigInteger bigInteger -> statement.setBigDecimal(i + 1, new BigDecimal(bigInteger));
            case Enum<?> enumValue -> statement.setString(i + 1, enumValue.name());
            case URL url -> statement.setURL(i + 1, url);
            case URI uri -> statement.setString(i + 1, uri.toString());
            case Path path -> statement.setString(i + 1, path.toString());
            case Blob blob -> statement.setBlob(i + 1, blob);
            case Clob clob -> statement.setClob(i + 1, clob);
            case byte[] bytes -> statement.setBytes(i + 1, bytes);
            case null -> statement.setNull(i + 1, Types.NULL);
            case String string -> statement.setString(i + 1, string);
            case StringBuilder sb -> statement.setString(i + 1, sb.toString());
            case StringBuffer sbf -> statement.setString(i + 1, sbf.toString());
            case CharSequence cs -> statement.setString(i + 1, cs.toString());
            case Byte byteParam -> statement.setByte(i + 1, byteParam);
            case Integer integer -> statement.setInt(i + 1, integer);
            case Short shortParam -> statement.setShort(i + 1, shortParam);
            case Long longParam -> statement.setLong(i + 1, longParam);
            case Float floatParam -> statement.setFloat(i + 1, floatParam);
            case Double doubleParam -> statement.setDouble(i + 1, doubleParam);
            case AtomicInteger atomicInt -> statement.setInt(i + 1, atomicInt.get());
            case AtomicLong atomicLong -> statement.setLong(i + 1, atomicLong.get());
            case AtomicBoolean atomicBool -> statement.setBoolean(i + 1, atomicBool.get());
            case Boolean booleanParam -> statement.setBoolean(i + 1, booleanParam);
            case Character character -> statement.setObject(i + 1, character);
            default -> {
                Class<?> aClass = param.getClass();
                Field field = TypeRegistry.FIELDS.get(aClass);

                try {
                    Object value = field.get(param);
                    statement.setObject(i + 1, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(
                            "Could not record the object of class: %s, you must manually specify its mapping"
                                    .formatted(aClass.getName()));
                }
            }
        }
    }

    static void validateArrayDefinition(String definition) {
        if (!SUPPORTED_ARRAY_TYPES.containsKey(definition.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("Unsupported array definition: " + definition);
    }

    static void validateArrayElementsMatchDefinition(Object[] array, String definition) {
        Class<?> expectedType = SUPPORTED_ARRAY_TYPES.get(definition.toLowerCase(Locale.ROOT));
        if (expectedType == null)
            throw new IllegalArgumentException("Cannot determine expected type for: " + definition);

        for (Object element : array) {
            if (element != null && !expectedType.isAssignableFrom(element.getClass()))
                throw new IllegalArgumentException("Element '%s' does not match expected type %s"
                        .formatted(element, expectedType.getSimpleName()));
        }
    }

    /**
     * Determines if a parameter is of a type that is directly supported by the setParameters method.
     *
     * @param param the parameter to check
     * @return true if the parameter type is supported, false otherwise
     */
    static boolean isSupportedType(final Object param) {
        return switch (param) {
            case String ignored -> true;
            case StringBuilder ignored -> true;
            case StringBuffer ignored -> true;
            case CharSequence ignored -> true;
            case Byte ignored -> true;
            case Integer ignored -> true;
            case Short ignored -> true;
            case Long ignored -> true;
            case Float ignored -> true;
            case Double ignored -> true;
            case Boolean ignored -> true;
            case Character ignored -> true;
            case UUID ignored -> true;
            case Time ignored -> true;
            case Timestamp ignored -> true;
            case LocalDateTime ignored -> true;
            case LocalDate ignored -> true;
            case LocalTime ignored -> true;
            case Instant ignored -> true;
            case ZonedDateTime ignored -> true;
            case OffsetDateTime ignored -> true;
            case Duration ignored -> true;
            case Period ignored -> true;
            case Year ignored -> true;
            case YearMonth ignored -> true;
            case MonthDay ignored -> true;
            case BigDecimal ignored -> true;
            case BigInteger ignored -> true;
            case AtomicInteger ignored -> true;
            case AtomicLong ignored -> true;
            case AtomicBoolean ignored -> true;
            case Enum<?> ignored -> true;
            case URL ignored -> true;
            case URI ignored -> true;
            case Path ignored -> true;
            case Blob ignored -> true;
            case Clob ignored -> true;
            case byte[] ignored -> true;
            case null -> true;
            default -> {
                Class<?> aClass = param.getClass();

                if (TypeRegistry.FIELDS.containsKey(aClass)) yield true;

                Field[] fields = getDeclaredInstanceFields(aClass);
                if (fields.length != 1) yield false;
                Field field = fields[0];

                try {
                    field.setAccessible(true);

                    Object value = field.get(param);
                    boolean supportedType = isSupportedChildType(value);
                    if (!supportedType) yield false;

                    TypeRegistry.FIELDS.put(aClass, field);
                    yield true;
                } catch (IllegalAccessException | InaccessibleObjectException | SecurityException e) {
                    yield false;
                }
            }
        };
    }

    private static Field[] getDeclaredInstanceFields(Class<?> clazz) {
        Field[] allFields = clazz.getDeclaredFields();
        return Arrays.stream(allFields)
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .toArray(Field[]::new);
    }

    static boolean isSupportedChildType(Object param) {
        return switch (param) {
            case String ignored -> true;
            case StringBuilder ignored -> true;
            case StringBuffer ignored -> true;
            case CharSequence ignored -> true;
            case Byte ignored -> true;
            case Integer ignored -> true;
            case Short ignored -> true;
            case Long ignored -> true;
            case Float ignored -> true;
            case Double ignored -> true;
            case Boolean ignored -> true;
            case Character ignored -> true;
            case UUID ignored -> true;
            case Time ignored -> true;
            case Timestamp ignored -> true;
            case LocalDateTime ignored -> true;
            case LocalDate ignored -> true;
            case LocalTime ignored -> true;
            case Instant ignored -> true;
            case ZonedDateTime ignored -> true;
            case OffsetDateTime ignored -> true;
            case Duration ignored -> true;
            case Period ignored -> true;
            case Year ignored -> true;
            case YearMonth ignored -> true;
            case MonthDay ignored -> true;
            case BigDecimal ignored -> true;
            case BigInteger ignored -> true;
            case AtomicInteger ignored -> true;
            case AtomicLong ignored -> true;
            case AtomicBoolean ignored -> true;
            case Enum<?> ignored -> true;
            case URL ignored -> true;
            case URI ignored -> true;
            case Path ignored -> true;
            case Blob ignored -> true;
            case Clob ignored -> true;
            case byte[] ignored -> true;
            case null -> true;
            default -> false;
        };
    }

    private static Map<Class<?>, Function<ResultSet, ?>> typesMapper() {
        return Map.ofEntries(
                Map.entry(String.class, rs -> {
                    try {
                        return rs.getString(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to String" + e.getMessage());
                    }
                }),
                Map.entry(Boolean.class, rs -> {
                    try {
                        return rs.getBoolean(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Boolean" + e.getMessage());
                    }
                }),
                Map.entry(Character.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        if (val == null || val.isEmpty())
                            throw new InvalidArgumentTypeException("Can't cast to Character: value is null or empty");

                        return val.charAt(0);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Character" + e.getMessage());
                    }
                }),
                Map.entry(Byte.class, rs -> {
                    try {
                        return rs.getByte(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Byte" + e.getMessage());
                    }
                }),
                Map.entry(Short.class, rs -> {
                    try {
                        return rs.getShort(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Short" + e.getMessage());
                    }
                }),
                Map.entry(Integer.class, rs -> {
                    try {
                        return rs.getInt(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Integer" + e.getMessage());
                    }
                }),
                Map.entry(Long.class, rs -> {
                    try {
                        return rs.getLong(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Long" + e.getMessage());
                    }
                }),
                Map.entry(Float.class, rs -> {
                    try {
                        return rs.getFloat(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Float" + e.getMessage());
                    }
                }),
                Map.entry(Double.class, rs -> {
                    try {
                        return rs.getDouble(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Double" + e.getMessage());
                    }
                }),
                Map.entry(UUID.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? UUID.fromString(val) : null;
                    } catch (SQLException | IllegalArgumentException e) {
                        throw new InvalidArgumentTypeException("Can't cast to UUID" + e.getMessage());
                    }
                }),
                Map.entry(StringBuilder.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? new StringBuilder(val) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to StringBuilder: " + e.getMessage());
                    }
                }),
                Map.entry(StringBuffer.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? new StringBuffer(val) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to StringBuffer: " + e.getMessage());
                    }
                }),
                Map.entry(CharSequence.class, rs -> {
                    try {
                        return rs.getString(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to CharSequence: " + e.getMessage());
                    }
                }),
                Map.entry(Time.class, rs -> {
                    try {
                        return rs.getTime(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Time: " + e.getMessage());
                    }
                }),
                Map.entry(AtomicInteger.class, rs -> {
                    try {
                        return new AtomicInteger(rs.getInt(1));
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to AtomicInteger: " + e.getMessage());
                    }
                }),
                Map.entry(AtomicLong.class, rs -> {
                    try {
                        return new AtomicLong(rs.getLong(1));
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to AtomicLong: " + e.getMessage());
                    }
                }),
                Map.entry(AtomicBoolean.class, rs -> {
                    try {
                        return new AtomicBoolean(rs.getBoolean(1));
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to AtomicBoolean: " + e.getMessage());
                    }
                }),
                Map.entry(Timestamp.class, rs -> {
                    try {
                        return rs.getTimestamp(1);
                    } catch (SQLException e) {
                         throw new InvalidArgumentTypeException("Can`t cast to timestamp" + e.getMessage());
                    }
                }),
                Map.entry(LocalDate.class, rs -> {
                    try {
                        Timestamp ts = rs.getTimestamp(1);
                        return ts != null ? ts.toLocalDateTime().toLocalDate() : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to LocalDate" + e.getMessage());
                    }
                }),
                Map.entry(LocalTime.class, rs -> {
                    try {
                        Time time = rs.getTime(1);
                        return time != null ? time.toLocalTime() : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to LocalTime" + e.getMessage());
                    }
                }),
                Map.entry(LocalDateTime.class, rs -> {
                    try {
                        Timestamp ts = rs.getTimestamp(1);
                        return ts != null ? ts.toLocalDateTime() : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to LocalDateTime" + e.getMessage());
                    }
                }),
                Map.entry(Instant.class, rs -> {
                    try {
                        Timestamp ts = rs.getTimestamp(1);
                        return ts != null ? ts.toInstant() : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Instant" + e.getMessage());
                    }
                }),
                Map.entry(ZonedDateTime.class, rs -> {
                    try {
                        Timestamp ts = rs.getTimestamp(1);
                        return ts != null ? ts.toLocalDateTime().atZone(ZoneId.systemDefault()) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to ZonedDateTime" + e.getMessage());
                    }
                }),
                Map.entry(OffsetDateTime.class, rs -> {
                    try {
                        Timestamp ts = rs.getTimestamp(1);
                        return ts != null ? ts.toLocalDateTime().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to OffsetDateTime" + e.getMessage());
                    }
                }),
                Map.entry(Duration.class, rs -> {
                    try {
                        long seconds = rs.getLong(1);
                        return Duration.ofSeconds(seconds);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Duration" + e.getMessage());
                    }
                }),
                Map.entry(Period.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? Period.parse(val) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Period" + e.getMessage());
                    }
                }),
                Map.entry(Year.class, rs -> {
                    try {
                        int year = rs.getInt(1);
                        return Year.of(year);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Year" + e.getMessage());
                    }
                }),
                Map.entry(YearMonth.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? YearMonth.parse(val) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to YearMonth" + e.getMessage());
                    }
                }),
                Map.entry(MonthDay.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? MonthDay.parse(val) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to MonthDay" + e.getMessage());
                    }
                }),
                Map.entry(BigDecimal.class, rs -> {
                    try {
                        return rs.getBigDecimal(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to BigDecimal" + e.getMessage());
                    }
                }),
                Map.entry(BigInteger.class, rs -> {
                    try {
                        BigDecimal val = rs.getBigDecimal(1);
                        return val != null ? val.toBigInteger() : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to BigInteger" + e.getMessage());
                    }
                }),
                Map.entry(URL.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? URI.create(val).toURL() : null;
                    } catch (SQLException | MalformedURLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to URL" + e.getMessage());
                    }
                }),
                Map.entry(URI.class, rs -> {
                    try {
                        String val = rs.getString(1);
                        return val != null ? URI.create(val) : null;
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to URI" + e.getMessage());
                    }
                }),
                Map.entry(Blob.class, rs -> {
                    try {
                        return rs.getBlob(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Blob" + e.getMessage());
                    }
                }),
                Map.entry(Clob.class, rs -> {
                    try {
                        return rs.getClob(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to Clob" + e.getMessage());
                    }
                }),
                Map.entry(byte[].class, rs -> {
                    try {
                        return rs.getBytes(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can't cast to byte[]" + e.getMessage());
                    }
                })
        );
    }
}
