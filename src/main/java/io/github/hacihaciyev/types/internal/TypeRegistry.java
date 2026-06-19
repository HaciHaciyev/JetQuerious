package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.types.AsObject;
import io.github.hacihaciyev.types.AsString;
import io.github.hacihaciyev.types.Getter;
import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.types.Setter;
import io.github.hacihaciyev.types.TypeInlineException;
import io.github.hacihaciyev.types.UUIDStrategy;

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

public final class TypeRegistry {

    public static final String UNSUPPORTED_RECORD =
            "Unsupported record type {%s}. If you want to use this record specify it`s package for build time meta data generation.";

    private static final ClassValue<TypeInfo> REGISTRY = new ClassValue<>() {
        @Override
        protected TypeInfo computeValue(Class<?> type) {
            return computeTypeInfo(type);
        }
    };

    private TypeRegistry() {}

    public static TypeInfo info(Class<?> type) {
        if (type == null) return TypeInfo.NONE;
        return REGISTRY.get(type);
    }

    private static TypeInfo computeTypeInfo(Class<?> type) {
        TypeInfo info = standardTypes(type);
        if (info instanceof TypeInfo.Some) return info;

        return tryMeta(type);
    }

    private static TypeInfo standardTypes(Class<?> type) {
        if (type == AsObject.class)
            return info(
                    (stmt, p, idx) -> stmt.setObject(idx, ((AsObject) p).value()),
                    (rs, col) -> rs.getObject(col)
            );

        if (type == AsString.class)
            return info(
                    (stmt, p, idx) -> stmt.setString(idx, String.valueOf(((AsString) p).value())),
                    (rs, col) -> rs.getString(col)
            );

        if (UUIDStrategy.class.isAssignableFrom(type))
            return info(
                    TypeRegistry::setUUID,
                    (rs, col) -> rs.getObject(col, UUID.class),
                    SQLType.UUID, SQLType.UNIQUEIDENTIFIER, SQLType.BINARY, SQLType.VARCHAR, SQLType.CHAR, SQLType.CHARACTER
            );

        if (type == UUID.class)
            return info(
                    (stmt, p, i) -> setUUID(stmt, Conf.INSTANCE.uuidStrategy().create((UUID) p), i),
                    (rs, col) -> rs.getObject(col, UUID.class),
                    SQLType.UUID, SQLType.UNIQUEIDENTIFIER, SQLType.BINARY, SQLType.VARCHAR, SQLType.CHAR, SQLType.CHARACTER
            );

        if (Enum.class.isAssignableFrom(type))
            return info(
                    (stmt, p, idx) -> stmt.setString(idx, ((Enum<?>) p).name()),
                    (rs, col) -> rs.getString(col),
                    charseqtypes()
            );

        if (type == String.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, (String) p),
                    (rs, col) -> rs.getString(col),
                    charseqtypes()
            );

        if (type == StringBuilder.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return s == null ? null : new StringBuilder(s);
                    },
                    charseqtypes()
            );

        if (type == StringBuffer.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return s == null ? null : new StringBuffer(s);
                    },
                    charseqtypes()
            );

        if (type == CharSequence.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> rs.getString(col),
                    charseqtypes()
            );

        if (type == Character.class || type == char.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, String.valueOf(p)),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return (s == null || s.isEmpty()) ? null : s.charAt(0);
                    },
                    charseqtypes()
            );

        if (type == URI.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return s == null ? null : URI.create(s);
                    },
                    charseqtypes()
            );

        if (type == URL.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        if (s == null) return null;
                        try {
                            return URI.create(s).toURL();
                        } catch (Exception e) {
                            throw new TypeInlineException(URL.class, e);
                        }
                    },
                    charseqtypes()
            );

        if (type == Path.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return s == null ? null : Path.of(s);
                    },
                    charseqtypes()
            );

        if (type == int.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, (int) p),
                    (rs, col) -> rs.getInt(col),
                    SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == Integer.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, (Integer) p),
                    (rs, col) -> rs.getInt(col),
                    SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == AtomicInteger.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, ((AtomicInteger) p).get()),
                    (rs, col) -> new AtomicInteger(rs.getInt(col)),
                    SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == long.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, (long) p),
                    (rs, col) -> rs.getLong(col),
                    SQLType.BIGINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == Long.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, (Long) p),
                    (rs, col) -> rs.getLong(col),
                    SQLType.BIGINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == AtomicLong.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, ((AtomicLong) p).get()),
                    (rs, col) -> new AtomicLong(rs.getLong(col)),
                    SQLType.BIGINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == short.class)
            return info(
                    (stmt, p, i) -> stmt.setShort(i, (short) p),
                    (rs, col) -> rs.getShort(col),
                    SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == Short.class)
            return info(
                    (stmt, p, i) -> stmt.setShort(i, (Short) p),
                    (rs, col) -> rs.getShort(col),
                    SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER, SQLType.BIGINT
            );

        if (type == byte.class)
            return info(
                    (stmt, p, i) -> stmt.setByte(i, (byte) p),
                    (rs, col) -> rs.getByte(col),
                    SQLType.TINYINT, SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == Byte.class)
            return info(
                    (stmt, p, i) -> stmt.setByte(i, (Byte) p),
                    (rs, col) -> rs.getByte(col),
                    SQLType.TINYINT, SQLType.SMALLINT, SQLType.INT, SQLType.INTEGER
            );

        if (type == double.class)
            return info(
                    (stmt, p, i) -> stmt.setDouble(i, (double) p),
                    (rs, col) -> rs.getDouble(col),
                    SQLType.DOUBLE, SQLType.DOUBLE_PRECISION, SQLType.FLOAT, SQLType.REAL
            );

        if (type == Double.class)
            return info(
                    (stmt, p, i) -> stmt.setDouble(i, (double) p),
                    (rs, col) -> rs.getDouble(col),
                    SQLType.DOUBLE, SQLType.DOUBLE_PRECISION, SQLType.FLOAT, SQLType.REAL
            );

        if (type == float.class)
            return info(
                    (stmt, p, i) -> stmt.setFloat(i, ((Number) p).floatValue()),
                    (rs, col) -> rs.getFloat(col),
                    SQLType.FLOAT, SQLType.REAL, SQLType.DOUBLE
            );

        if (type == Float.class)
            return info(
                    (stmt, p, i) -> stmt.setFloat(i, (Float) p),
                    (rs, col) -> rs.getFloat(col),
                    SQLType.FLOAT, SQLType.REAL, SQLType.DOUBLE
            );

        if (type == BigDecimal.class)
            return info(
                    (stmt, p, i) -> stmt.setBigDecimal(i, (BigDecimal) p),
                    (rs, col) -> rs.getBigDecimal(col),
                    SQLType.DECIMAL, SQLType.NUMERIC, SQLType.MONEY, SQLType.SMALLMONEY,
                    SQLType.FLOAT, SQLType.DOUBLE
            );

        if (type == BigInteger.class)
            return info(
                    (stmt, p, i) -> stmt.setBigDecimal(i, new BigDecimal((BigInteger) p)),
                    (rs, col) -> {
                        var bd = rs.getBigDecimal(col);
                        return bd == null ? null : bd.toBigInteger();
                    },
                    SQLType.DECIMAL, SQLType.NUMERIC, SQLType.BIGINT
            );

        if (type == boolean.class || type == Boolean.class)
            return info(
                    (stmt, p, i) -> stmt.setBoolean(i, (Boolean) p),
                    (rs, col) -> rs.getBoolean(col),
                    SQLType.BOOLEAN, SQLType.BIT
            );

        if (type == AtomicBoolean.class)
            return info(
                    (stmt, p, i) -> stmt.setBoolean(i, ((AtomicBoolean) p).get()),
                    (rs, col) -> new AtomicBoolean(rs.getBoolean(col)),
                    SQLType.BOOLEAN, SQLType.BIT
            );

        if (type == byte[].class)
            return info(
                    (stmt, p, i) -> stmt.setBytes(i, (byte[]) p),
                    (rs, col) -> rs.getBytes(col),
                    SQLType.BINARY, SQLType.VARBINARY, SQLType.BINARY_VARYING,
                    SQLType.BLOB, SQLType.ROWVERSION
            );

        if (type == Blob.class)
            return info(
                    (stmt, p, i) -> stmt.setBlob(i, (Blob) p),
                    (rs, col) -> rs.getBlob(col),
                    SQLType.BLOB, SQLType.BINARY, SQLType.VARBINARY
            );

        if (type == Clob.class)
            return info(
                    (stmt, p, i) -> stmt.setClob(i, (Clob) p),
                    (rs, col) -> rs.getClob(col),
                    SQLType.CLOB, SQLType.TEXT
            );

        if (type == Timestamp.class)
            return info(
                    (stmt, p, i) -> stmt.setTimestamp(i, (Timestamp) p),
                    (rs, col) -> rs.getTimestamp(col),
                    SQLType.TIMESTAMP, SQLType.DATETIME, SQLType.SMALLDATETIME,
                    SQLType.TIMESTAMP_WITHOUT_TIME_ZONE
            );

        if (type == LocalDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    (rs, col) -> rs.getObject(col, LocalDateTime.class),
                    SQLType.TIMESTAMP, SQLType.DATETIME, SQLType.DATETIME2,
                    SQLType.TIMESTAMP_WITHOUT_TIME_ZONE
            );

        if (type == LocalDate.class)
            return info(
                    (stmt, p, i) -> stmt.setDate(i, Date.valueOf((LocalDate) p)),
                    (rs, col) -> rs.getObject(col, LocalDate.class),
                    SQLType.DATE
            );

        if (type == LocalTime.class)
            return info(
                    (stmt, p, i) -> stmt.setTime(i, Time.valueOf((LocalTime) p)),
                    (rs, col) -> rs.getObject(col, LocalTime.class),
                    SQLType.TIME
            );

        if (type == Instant.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p, JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    (rs, col) -> {
                        var odt = rs.getObject(col, OffsetDateTime.class);
                        return odt == null ? null : odt.toInstant();
                    },
                    SQLType.TIMESTAMP_WITH_TIME_ZONE, SQLType.DATETIMEOFFSET
            );

        if (type == OffsetDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p, JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    (rs, col) -> rs.getObject(col, OffsetDateTime.class),
                    SQLType.TIMESTAMP_WITH_TIME_ZONE, SQLType.DATETIMEOFFSET
            );

        if (type == ZonedDateTime.class)
            return info(
                    (stmt, p, i) ->
                            stmt.setObject(i, ((ZonedDateTime) p).toOffsetDateTime(), JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    (rs, col) -> {
                        var odt = rs.getObject(col, OffsetDateTime.class);
                        return odt == null ? null : odt.toZonedDateTime();
                    },
                    SQLType.TIMESTAMP_WITH_TIME_ZONE, SQLType.DATETIMEOFFSET
            );

        if (type == Time.class)
            return info(
                    (stmt, p, i) -> stmt.setTime(i, (Time) p),
                    (rs, col) -> rs.getTime(col),
                    SQLType.TIME
            );

        if (type == Date.class)
            return info(
                    (stmt, p, i) -> stmt.setDate(i, (Date) p),
                    (rs, col) -> rs.getDate(col),
                    SQLType.DATE
            );

        if (type == Duration.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    (rs, col) -> rs.getObject(col, Duration.class),
                    SQLType.INTERVAL
            );

        if (type == Period.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    (rs, col) -> rs.getObject(col, Period.class),
                    SQLType.INTERVAL
            );

        if (type == Year.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, ((Year) p).getValue()),
                    (rs, col) -> Year.of(rs.getInt(col)),
                    SQLType.YEAR, SQLType.INT, SQLType.SMALLINT
            );

        if (type == YearMonth.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return s == null ? null : YearMonth.parse(s);
                    },
                    charseqtypes()
            );

        if (type == MonthDay.class)
            return info(
                    (stmt, p, i) -> stmt.setString(i, p.toString()),
                    (rs, col) -> {
                        var s = rs.getString(col);
                        return s == null ? null : MonthDay.parse(s);
                    },
                    charseqtypes()
            );

        if (type == Void.class)
            return info(
                    (stmt, p, i) -> stmt.setNull(i, Types.NULL),
                    (rs, col) -> null,
                    SQLType.NULL, SQLType.CURSOR, SQLType.TABLE_TYPE
            );

        return TypeInfo.NONE;
    }

    private static TypeInfo info(Setter setter, Getter getter, SQLType... sqlTypes) {
        return new TypeInfo.Some(setter, getter, Set.of(sqlTypes));
    }

    private static SQLType[] charseqtypes() {
        return new SQLType[]{
                SQLType.VARCHAR, SQLType.TEXT, SQLType.CHAR, SQLType.CHARACTER,
                SQLType.NCHAR, SQLType.NVARCHAR, SQLType.CHARACTER_VARYING,
                SQLType.NATIONAL_CHAR, SQLType.NATIONAL_CHAR_VARYING,
                SQLType.XML, SQLType.JSON, SQLType.JSONB, SQLType.HIERARCHYID
        };
    }

    private static void setUUID(PreparedStatement stmt, Object param, int idx) throws SQLException, TypeInlineException {
        switch ((UUIDStrategy) param) {
            case UUIDStrategy.Native(var uuid) -> stmt.setObject(idx, uuid);
            case UUIDStrategy.Charseq(var uuid) -> {
                var charseq = uuid.toString();

                var typeInfo = REGISTRY.get(charseq.getClass());
                if (typeInfo instanceof TypeInfo.Some typeSetter)
                    typeSetter.setter().set(stmt, charseq, idx);
            }
            case UUIDStrategy.Binary(var uuid) -> {
                var bytes = new byte[16];
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

                var typeInfo = REGISTRY.get(bytes.getClass());
                if (typeInfo instanceof TypeInfo.Some typeSetter)
                    typeSetter.setter().set(stmt, bytes, idx);
            }
        }
    }

    private static TypeInfo tryMeta(Class<?> type) {
        return switch (MetaRegistry.meta(type)) {
            case TypeMeta.Record<?> rec -> singleValueRecord(rec);
            case TypeMeta.None _ -> TypeInfo.NONE;
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeInfo singleValueRecord(TypeMeta.Record<T> rec) {
        if (rec.fields().length != 1) return TypeInfo.NONE;

        var field = rec.fields()[0];

        var fieldInfo = standardTypes(field.type());
        if (!(fieldInfo instanceof TypeInfo.Some(var setter, var getter, var sqlTypes))) return TypeInfo.NONE;

        Setter recordSetter = (stmt, p, idx) -> {
            try {
                var fieldValue = field.accessor().apply((T) p);
                setter.set(stmt, fieldValue, idx);
            } catch (SQLException e) {
                throw e;
            } catch (Throwable e) {
                throw new TypeInlineException(rec.type(), e);
            }
        };

        Getter recordGetter = (rs, col) -> {
            try {
                var fieldValue = getter.get(rs, col);
                return rec.factory().create(fieldValue);
            } catch (SQLException e) {
                throw e;
            } catch (Throwable e) {
                throw new TypeInlineException(rec.type(), e);
            }
        };

        return new TypeInfo.WithFactory<>(recordSetter, recordGetter, sqlTypes, rec.fields(), rec.factory());
    }
}