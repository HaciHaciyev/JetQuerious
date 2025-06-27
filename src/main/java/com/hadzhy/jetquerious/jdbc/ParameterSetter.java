package com.hadzhy.jetquerious.jdbc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ParameterSetter {

    private ParameterSetter() {}

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
}
