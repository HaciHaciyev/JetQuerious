package io.github.hacihaciyev.jdbc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class ParameterSetter {

    static final ClassValue<Setter> SETTERS = new ClassValue<>() {
        @Override
        protected Setter computeValue(Class<?> type) {
            return computeSetters(type);
        }
    };

    private ParameterSetter() {}

    static void setParameter(final PreparedStatement stmt, final Object param, final int idx) throws SQLException {
        if (param == null) {
            stmt.setNull(idx, Types.NULL);
            return;
        }
        Setter setter = SETTERS.get(param.getClass());
        if (setter == null) {
            if (param instanceof Enum<?>) {
                stmt.setString(idx, ((Enum<?>) param).name());
                return;
            }

            setValueObjectType(stmt, param, idx);
            return;
        }

        setter.set(stmt, param, idx);
    }

    public static void setParameter(
            final PreparedStatement stmt,
            final Object param,
            final int idx,
            final UUIDStrategy uuidStrategy) throws SQLException {

        if (param instanceof UUID uuid) {
            setUUID(stmt, uuid, idx, uuidStrategy);
            return;
        }

        setParameter(stmt, param, idx);
    }


    private static void setValueObjectType(final PreparedStatement statement, final Object param, final int i) throws SQLException {
        Class<?> aClass = param.getClass();
        Field field = TypeRegistry.FIELDS.get(aClass);

        try {
            Object value = field.get(param);
            if (value == null) {
                statement.setNull(i, Types.NULL);
                return;
            }

            setParameter(statement, value, i);
        } catch (IllegalAccessException | IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Could not record the object of class: %s, you must manually specify its mapping"
                            .formatted(aClass.getName()));
        }
    }

    private static Setter computeSetters(Class<?> type) {
        if (type == String.class) return (statement, param, index) -> statement.setString(index, (String) param);
        if (type == StringBuilder.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == StringBuffer.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == CharSequence.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == Character.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == int.class || type == Integer.class) return (statement, param, index) -> statement.setInt(index, (Integer) param);
        if (type == long.class || type == Long.class) return (statement, param, index) -> statement.setLong(index, (Long) param);
        if (type == double.class || type == Double.class) return (statement, param, index) -> statement.setDouble(index, (Double) param);
        if (type == float.class || type == Float.class) return (statement, param, index) -> statement.setFloat(index, (Float) param);
        if (type == BigDecimal.class) return (statement, param, index) -> statement.setBigDecimal(index, (BigDecimal) param);
        if (type == BigInteger.class) return (statement, param, index) -> statement.setBigDecimal(index, new BigDecimal((BigInteger) param));
        if (type == short.class || type == Short.class) return (statement, param, index) -> statement.setShort(index, (Short) param);
        if (type == byte.class || type == Byte.class) return (statement, param, index) -> statement.setByte(index, (Byte) param);
        if (type == AtomicInteger.class) return (statement, param, index) -> statement.setInt(index, ((AtomicInteger) param).get());
        if (type == AtomicLong.class) return (statement, param, index) -> statement.setLong(index, ((AtomicLong) param).get());
        if (type == Boolean.class) return (statement, param, index) -> statement.setBoolean(index, (Boolean) param);
        if (type == AtomicBoolean.class) return (statement, param, index) -> statement.setBoolean(index, ((AtomicBoolean) param).get());
        if (type == UUID.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == byte[].class) return (statement, param, index) -> statement.setBytes(index, (byte[]) param);
        if (type == Blob.class) return (statement, param, index) -> statement.setBlob(index, (Blob) param);
        if (type == Clob.class) return (statement, param, index) -> statement.setClob(index, (Clob) param);
        if (type == Timestamp.class) return (statement, param, index) -> statement.setTimestamp(index, (Timestamp) param);
        if (type == LocalDateTime.class) return (statement, param, index) -> statement.setObject(index, Timestamp.valueOf((LocalDateTime) param));
        if (type == LocalDate.class) return (statement, param, index) -> statement.setObject(index, java.sql.Date.valueOf((LocalDate) param));
        if (type == LocalTime.class) return (statement, param, index) -> statement.setObject(index, Time.valueOf((LocalTime) param));
        if (type == Instant.class) return (statement, param, index) -> statement.setObject(index, Timestamp.from((Instant) param));
        if (type == OffsetDateTime.class) return (statement, param, index) -> statement.setObject(index, Timestamp.from(((OffsetDateTime) param).toInstant()));
        if (type == ZonedDateTime.class) return (statement, param, index) -> statement.setObject(index, Timestamp.from(((ZonedDateTime) param).toInstant()));
        if (type == Time.class) return (statement, param, index) -> statement.setTime(index, (Time) param);
        if (type == Duration.class) return (statement, param, index) -> statement.setObject(index, param);
        if (type == Period.class) return (statement, param, index) -> statement.setObject(index, param);
        if (type == Year.class) return (statement, param, index) -> statement.setInt(index, ((Year) param).getValue());
        if (type == YearMonth.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == MonthDay.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == URL.class) return (statement, param, index) -> statement.setURL(index, (URL) param);
        if (type == URI.class) return (statement, param, index) -> statement.setString(index, param.toString());
        if (type == Path.class) return (statement, param, index) -> statement.setString(index, param.toString());
        return null;
    }

    private static void setUUID(PreparedStatement stmt, UUID uuid, int idx, UUIDStrategy uuidStrategy) throws SQLException {
        switch (uuidStrategy) {
            case STRING -> stmt.setString(idx, uuid.toString());
            case OBJECT -> stmt.setObject(idx, uuid);
            case BINARY -> {
                ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
                bb.putLong(uuid.getMostSignificantBits());
                bb.putLong(uuid.getLeastSignificantBits());
                stmt.setBytes(idx, bb.array());
            }
        }
    }

    enum UUIDStrategy {
        STRING,
        BINARY,
        OBJECT
    }
}
