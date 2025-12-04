package io.github.hacihaciyev.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class JavaTypeRegistry {

    static final ClassValue<TypeInfo> REGISTRY = new ClassValue<>() {
        @Override
        protected TypeInfo computeValue(Class<?> type) {
            return computeTypeInfo(type);
        }
    };

    private JavaTypeRegistry() {}

    public static TypeInfo get(Class<?> type) {
        return REGISTRY.get(type);
    }

    public record TypeInfo(Setter setter, Set<SQLType> sqlTypes) {
        public TypeInfo {
            sqlTypes = Set.copyOf(sqlTypes);
        }
    }

    private static TypeInfo computeTypeInfo(Class<?> type) {

        if (type == AsObject.class)
            return info(
                    (stmt, param, idx) -> stmt.setObject(idx, type)
            );

        if (type == AsString.class)
            return info(
                    (stmt, param, idx) -> stmt.setString(idx, type.toString())
            );

        if (UUIDStrategy.class.isAssignableFrom(type))
            return info(
                    JavaTypeRegistry::setUUID,
                    SQLType.UUID, SQLType.UNIQUEIDENTIFIER, SQLType.BINARY, SQLType.VARCHAR, SQLType.CHAR, SQLType.CHARACTER
            );

        if (type == String.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, (String) p),
                    charseqtypes()
            );

        if (type == StringBuilder.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == StringBuffer.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == CharSequence.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == Character.class || type == char.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, String.valueOf(p)),
                    charseqtypes()
            );

        if (type == URI.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == URL.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == Path.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == int.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, (int) p),
                    SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == Integer.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, (Integer) p),
                    SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == AtomicInteger.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, ((AtomicInteger) p).get()),
                    SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == long.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, (long) p),
                    SQLType.BIGINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == Long.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, (Long) p),
                    SQLType.BIGINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == AtomicLong.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, ((AtomicLong) p).get()),
                    SQLType.BIGINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == short.class)
            return info(
                    (stmt, p, i) -> stmt.setShort(i, (short) p),
                    SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == Short.class)
            return info(
                    (stmt, p, i) -> stmt.setShort(i, (Short) p),
                    SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == byte.class)
            return info(
                    (stmt, p, i) -> stmt.setByte(i, (byte) p),
                    SQLType.TINYINT, SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == Byte.class)
            return info(
                    (stmt, p, i) -> stmt.setByte(i, (Byte) p),
                    SQLType.TINYINT, SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == double.class)
            return info(
                    (stmt, p, i) -> stmt.setDouble(i, (double) p),
                    SQLType.DOUBLE, SQLType.DOUBLE_PRECISION, SQLType.FLOAT, SQLType.REAL
            );

        if (type == Double.class)
            return info(
                    (stmt, p, i) -> stmt.setDouble(i, (double) p),
                    SQLType.DOUBLE, SQLType.DOUBLE_PRECISION, SQLType.FLOAT, SQLType.REAL
            );

        if (type == float.class)
            return info(
                    (stmt, p, i) -> stmt.setFloat(i, ((Number) p).floatValue()),
                    SQLType.FLOAT, SQLType.REAL, SQLType.DOUBLE
            );

        if (type == Float.class)
            return info(
                    (stmt, p, i) -> stmt.setFloat(i, (Float) p),
                    SQLType.FLOAT, SQLType.REAL, SQLType.DOUBLE
            );

        if (type == BigDecimal.class)
            return info(
                    (stmt, p, i) -> stmt.setBigDecimal(i, (BigDecimal) p),
                    SQLType.DECIMAL, SQLType.NUMERIC, SQLType.MONEY, SQLType.SMALLMONEY,
                    SQLType.FLOAT, SQLType.DOUBLE
            );

        if (type == BigInteger.class)
            return info(
                    (stmt, p, i) -> stmt.setBigDecimal(i, new BigDecimal((BigInteger) p)),
                    SQLType.DECIMAL, SQLType.NUMERIC, SQLType.BIGINT
            );

        if (type == boolean.class || type == Boolean.class)
            return info(
                    (stmt, p, i) -> stmt.setBoolean(i, (Boolean) p),
                    SQLType.BOOLEAN, SQLType.BIT
            );

        if (type == AtomicBoolean.class)
            return info(
                    (stmt, p, i) -> stmt.setBoolean(i, ((AtomicBoolean) p).get()),
                    SQLType.BOOLEAN, SQLType.BIT
            );

        if (type == UUID.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    SQLType.UUID, SQLType.UNIQUEIDENTIFIER
            );

        if (type == byte[].class)
            return info(
                    (stmt, p, i) -> stmt.setBytes(i, (byte[]) p),
                    SQLType.BINARY, SQLType.VARBINARY, SQLType.BINARY_VARYING,
                    SQLType.BLOB, SQLType.ROWVERSION
            );

        if (type == Blob.class)
            return info(
                    (stmt, p, i) -> stmt.setBlob(i, (Blob) p),
                    SQLType.BLOB, SQLType.BINARY, SQLType.VARBINARY
            );

        if (type == Clob.class)
            return info(
                    (stmt, p, i) -> stmt.setClob(i, (Clob) p),
                    SQLType.CLOB, SQLType.TEXT
            );

        if (type == Timestamp.class)
            return info(
                    (stmt, p, i) -> stmt.setTimestamp(i, (Timestamp) p),
                    SQLType.TIMESTAMP, SQLType.DATETIME, SQLType.SMALLDATETIME,
                    SQLType.TIMESTAMP_WITHOUT_TIME_ZONE
            );

        if (type == LocalDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    SQLType.TIMESTAMP, SQLType.DATETIME, SQLType.DATETIME2,
                    SQLType.TIMESTAMP_WITHOUT_TIME_ZONE
            );

        if (type == LocalDate.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, Date.valueOf((LocalDate) p)),
                    SQLType.DATE
            );

        if (type == LocalTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, Time.valueOf((LocalTime) p)),
                    SQLType.TIME
            );

        if (type == Instant.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    SQLType.TIMESTAMP, SQLType.TIMESTAMP_WITH_TIME_ZONE
            );

        if (type == OffsetDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    SQLType.TIMESTAMP_WITH_TIME_ZONE, SQLType.DATETIMEOFFSET, SQLType.TIMESTAMP
            );

        if (type == ZonedDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, ((ZonedDateTime) p).toOffsetDateTime()),
                    SQLType.TIMESTAMP_WITH_TIME_ZONE, SQLType.TIMESTAMP
            );

        if (type == Time.class)
            return info(
                    (stmt, p, i) -> stmt.setTime(i, (Time) p),
                    SQLType.TIME
            );

        if (type == Date.class)
            return info(
                    (stmt, p, i) -> stmt.setDate(i, (Date) p),
                    SQLType.DATE
            );

        if (type == Duration.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    SQLType.INTERVAL
            );

        if (type == Period.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    SQLType.INTERVAL
            );

        if (type == Year.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, ((Year) p).getValue()),
                    SQLType.YEAR, SQLType.INT, SQLType.SMALLINT
            );

        if (type == YearMonth.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == MonthDay.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    charseqtypes()
            );

        if (type == Void.class)
            return info(
                    (stmt, p, i) -> stmt.setNull(i, Types.NULL),
                    SQLType.NULL, SQLType.CURSOR, SQLType.TABLE_TYPE
            );

        return null;
    }

    private static TypeInfo info(Setter setter, SQLType... sqlTypes) {
        return new TypeInfo(setter, Set.of(sqlTypes));
    }

    private static SQLType[] charseqtypes() {
        return new SQLType[]{
                SQLType.VARCHAR, SQLType.TEXT, SQLType.CHAR, SQLType.CHARACTER,
                SQLType.NCHAR, SQLType.NVARCHAR, SQLType.CHARACTER_VARYING,
                SQLType.NATIONAL_CHAR, SQLType.NATIONAL_CHAR_VARYING,
                SQLType.XML, SQLType.JSON, SQLType.JSONB, SQLType.HIERARCHYID
        };
    }

    private static void setUUID(PreparedStatement stmt, Object param, int idx) throws SQLException {
        UUIDStrategy strategy = (UUIDStrategy) param;

        var value = switch (strategy) {
            case UUIDStrategy.Native n -> n.value();
            case UUIDStrategy.Charseq c -> c.value().toString();
            case UUIDStrategy.Binary b -> {
                byte[] bytes = new byte[16];
                UUID uuid = b.value();
                long msb = uuid.getMostSignificantBits();
                long lsb = uuid.getLeastSignificantBits();

                bytes[0] = (byte) (msb >>> 56);
                bytes[1] = (byte) (msb >>> 48);
                bytes[2] = (byte) (msb >>> 40);
                bytes[3] = (byte) (msb >>> 32);
                bytes[4] = (byte) (msb >>> 24);
                bytes[5] = (byte) (msb >>> 16);
                bytes[6] = (byte) (msb >>> 8);
                bytes[7] = (byte) (msb);

                bytes[8] = (byte) (lsb >>> 56);
                bytes[9] = (byte) (lsb >>> 48);
                bytes[10] = (byte) (lsb >>> 40);
                bytes[11] = (byte) (lsb >>> 32);
                bytes[12] = (byte) (lsb >>> 24);
                bytes[13] = (byte) (lsb >>> 16);
                bytes[14] = (byte) (lsb >>> 8);
                bytes[15] = (byte) (lsb);

                yield bytes;
            }
        };

        REGISTRY.get(value.getClass()).setter().set(stmt, value, idx);
    }
}
