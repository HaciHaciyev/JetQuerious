package io.github.hacihaciyev.types;

import java.lang.invoke.MethodHandle;
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

    static final ClassValue<Setter> SETTERS = new ClassValue<>() {
        @Override
        protected Setter computeValue(Class<?> type) {
            return computeSetters(type);
        }
    };

    private ParameterSetter() {}

    public static void setParameter(final PreparedStatement stmt, final Object param, final int idx) throws SQLException {
        if (param == null) {
            stmt.setNull(idx, Types.NULL);
            return;
        }

        if (setSimpleValue(stmt, param, idx)) return;
        setValueObjectType(stmt, param, idx);
    }

    private static boolean setSimpleValue(PreparedStatement stmt, Object param, int idx) throws SQLException {
        Setter setter = SETTERS.get(param.getClass());
        if (setter != null) {
            setter.set(stmt, param, idx);
            return true;
        }

        if (param instanceof Enum<?>) {
            stmt.setString(idx, ((Enum<?>) param).name());
            return true;
        }

        return false;
    }

    private static void setValueObjectType(final PreparedStatement statement, final Object param, final int i) {
        Class<?> aClass = param.getClass();
        MethodHandle accessor = TypeRegistry.RECORD_ACCESSORS.get(aClass);

        if (accessor == null)
            throw new IllegalArgumentException(
                    "Could not find accessor for record class: %s, you must manually specify its mapping"
                            .formatted(aClass.getName()));

        try {
            Object value = accessor.invoke(param);
            if (value == null) {
                statement.setNull(i, java.sql.Types.NULL);
                return;
            }

            setSimpleValue(statement, value, i);
        } catch (Throwable e) {
            throw new IllegalArgumentException(
                    "Could not read the value of record class: %s, you must manually specify its mapping"
                            .formatted(aClass.getName()), e);
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
        if (UUIDStrategy.class.isAssignableFrom(type)) return setUUID();
        if (type == UUID.class) return (statement, param, index) -> statement.setObject(index, param);
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
        if (type == Void.class) return (statement, param, index) -> statement.setNull(index, Types.NULL);
        return null;
    }

    private static Setter setUUID() {
        return (statement, param, index) -> {
            UUIDStrategy strategy = (UUIDStrategy) param;

            Object value = switch (strategy) {
                case UUIDStrategy.Native n -> n.value();
                case UUIDStrategy.Charseq c -> c.value().toString();
                case UUIDStrategy.Binary b -> {
                    byte[] bytes = new byte[16];
                    writeUUIDToBytes(b.value(), bytes);
                    yield bytes;
                }
            };

            setSimpleValue(statement, value, index);
        };
    }

    private static void writeUUIDToBytes(UUID uuid, byte[] dest) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        dest[0] = (byte) (msb >>> 56);
        dest[1] = (byte) (msb >>> 48);
        dest[2] = (byte) (msb >>> 40);
        dest[3] = (byte) (msb >>> 32);
        dest[4] = (byte) (msb >>> 24);
        dest[5] = (byte) (msb >>> 16);
        dest[6] = (byte) (msb >>> 8);
        dest[7] = (byte) (msb);

        dest[8] = (byte) (lsb >>> 56);
        dest[9] = (byte) (lsb >>> 48);
        dest[10] = (byte) (lsb >>> 40);
        dest[11] = (byte) (lsb >>> 32);
        dest[12] = (byte) (lsb >>> 24);
        dest[13] = (byte) (lsb >>> 16);
        dest[14] = (byte) (lsb >>> 8);
        dest[15] = (byte) (lsb);
    }
}
