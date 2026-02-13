package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.types.AsObject;
import io.github.hacihaciyev.types.AsString;
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
                    (stmt, p, idx) -> stmt.setObject(idx, ((AsObject) p).value())
            );

        if (type == AsString.class)
            return info(
                    (stmt, p, idx) -> stmt.setString(idx, String.valueOf(((AsString) p).value()))
            );

        if (UUIDStrategy.class.isAssignableFrom(type))
            return info(
                    TypeRegistry::setUUID,
                    io.github.hacihaciyev.types.SQLType.UUID, io.github.hacihaciyev.types.SQLType.UNIQUEIDENTIFIER, io.github.hacihaciyev.types.SQLType.BINARY, io.github.hacihaciyev.types.SQLType.VARCHAR, io.github.hacihaciyev.types.SQLType.CHAR, io.github.hacihaciyev.types.SQLType.CHARACTER
            );

        if (type == UUID.class)
            return info(
                    (stmt, p, i) -> setUUID(stmt, Conf.INSTANCE.uuidStrategy().create((UUID) p), i),
                    io.github.hacihaciyev.types.SQLType.UUID, io.github.hacihaciyev.types.SQLType.UNIQUEIDENTIFIER, io.github.hacihaciyev.types.SQLType.BINARY, io.github.hacihaciyev.types.SQLType.VARCHAR, io.github.hacihaciyev.types.SQLType.CHAR, io.github.hacihaciyev.types.SQLType.CHARACTER
            );

        if (Enum.class.isAssignableFrom(type))
            return info(
                    (stmt, p, idx) -> stmt.setString(idx, ((Enum<?>) p).name()),
                    charseqtypes()
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
                    io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER, io.github.hacihaciyev.types.SQLType.BIGINT
            );

        if (type == Integer.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, (Integer) p),
                    io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER, io.github.hacihaciyev.types.SQLType.BIGINT
            );

        if (type == AtomicInteger.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, ((AtomicInteger) p).get()),
                    io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER, io.github.hacihaciyev.types.SQLType.BIGINT
            );

        if (type == long.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, (long) p),
                    io.github.hacihaciyev.types.SQLType.BIGINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER
            );

        if (type == Long.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, (Long) p),
                    io.github.hacihaciyev.types.SQLType.BIGINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER
            );

        if (type == AtomicLong.class)
            return info(
                    (stmt, p, i) -> stmt.setLong(i, ((AtomicLong) p).get()),
                    io.github.hacihaciyev.types.SQLType.BIGINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER
            );

        if (type == short.class)
            return info(
                    (stmt, p, i) -> stmt.setShort(i, (short) p),
                    io.github.hacihaciyev.types.SQLType.SMALLINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER, io.github.hacihaciyev.types.SQLType.BIGINT
            );

        if (type == Short.class)
            return info(
                    (stmt, p, i) -> stmt.setShort(i, (Short) p),
                    io.github.hacihaciyev.types.SQLType.SMALLINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER, io.github.hacihaciyev.types.SQLType.BIGINT
            );

        if (type == byte.class)
            return info(
                    (stmt, p, i) -> stmt.setByte(i, (byte) p),
                    io.github.hacihaciyev.types.SQLType.TINYINT, io.github.hacihaciyev.types.SQLType.SMALLINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER
            );

        if (type == Byte.class)
            return info(
                    (stmt, p, i) -> stmt.setByte(i, (Byte) p),
                    io.github.hacihaciyev.types.SQLType.TINYINT, io.github.hacihaciyev.types.SQLType.SMALLINT, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.INTEGER
            );

        if (type == double.class)
            return info(
                    (stmt, p, i) -> stmt.setDouble(i, (double) p),
                    io.github.hacihaciyev.types.SQLType.DOUBLE, io.github.hacihaciyev.types.SQLType.DOUBLE_PRECISION, io.github.hacihaciyev.types.SQLType.FLOAT, io.github.hacihaciyev.types.SQLType.REAL
            );

        if (type == Double.class)
            return info(
                    (stmt, p, i) -> stmt.setDouble(i, (double) p),
                    io.github.hacihaciyev.types.SQLType.DOUBLE, io.github.hacihaciyev.types.SQLType.DOUBLE_PRECISION, io.github.hacihaciyev.types.SQLType.FLOAT, io.github.hacihaciyev.types.SQLType.REAL
            );

        if (type == float.class)
            return info(
                    (stmt, p, i) -> stmt.setFloat(i, ((Number) p).floatValue()),
                    io.github.hacihaciyev.types.SQLType.FLOAT, io.github.hacihaciyev.types.SQLType.REAL, io.github.hacihaciyev.types.SQLType.DOUBLE
            );

        if (type == Float.class)
            return info(
                    (stmt, p, i) -> stmt.setFloat(i, (Float) p),
                    io.github.hacihaciyev.types.SQLType.FLOAT, io.github.hacihaciyev.types.SQLType.REAL, io.github.hacihaciyev.types.SQLType.DOUBLE
            );

        if (type == BigDecimal.class)
            return info(
                    (stmt, p, i) -> stmt.setBigDecimal(i, (BigDecimal) p),
                    io.github.hacihaciyev.types.SQLType.DECIMAL, io.github.hacihaciyev.types.SQLType.NUMERIC, io.github.hacihaciyev.types.SQLType.MONEY, io.github.hacihaciyev.types.SQLType.SMALLMONEY,
                    io.github.hacihaciyev.types.SQLType.FLOAT, io.github.hacihaciyev.types.SQLType.DOUBLE
            );

        if (type == BigInteger.class)
            return info(
                    (stmt, p, i) -> stmt.setBigDecimal(i, new BigDecimal((BigInteger) p)),
                    io.github.hacihaciyev.types.SQLType.DECIMAL, io.github.hacihaciyev.types.SQLType.NUMERIC, io.github.hacihaciyev.types.SQLType.BIGINT
            );

        if (type == boolean.class || type == Boolean.class)
            return info(
                    (stmt, p, i) -> stmt.setBoolean(i, (Boolean) p),
                    io.github.hacihaciyev.types.SQLType.BOOLEAN, io.github.hacihaciyev.types.SQLType.BIT
            );

        if (type == AtomicBoolean.class)
            return info(
                    (stmt, p, i) -> stmt.setBoolean(i, ((AtomicBoolean) p).get()),
                    io.github.hacihaciyev.types.SQLType.BOOLEAN, io.github.hacihaciyev.types.SQLType.BIT
            );

        if (type == byte[].class)
            return info(
                    (stmt, p, i) -> stmt.setBytes(i, (byte[]) p),
                    io.github.hacihaciyev.types.SQLType.BINARY, io.github.hacihaciyev.types.SQLType.VARBINARY, io.github.hacihaciyev.types.SQLType.BINARY_VARYING,
                    io.github.hacihaciyev.types.SQLType.BLOB, io.github.hacihaciyev.types.SQLType.ROWVERSION
            );

        if (type == Blob.class)
            return info(
                    (stmt, p, i) -> stmt.setBlob(i, (Blob) p),
                    io.github.hacihaciyev.types.SQLType.BLOB, io.github.hacihaciyev.types.SQLType.BINARY, io.github.hacihaciyev.types.SQLType.VARBINARY
            );

        if (type == Clob.class)
            return info(
                    (stmt, p, i) -> stmt.setClob(i, (Clob) p),
                    io.github.hacihaciyev.types.SQLType.CLOB, io.github.hacihaciyev.types.SQLType.TEXT
            );

        if (type == Timestamp.class)
            return info(
                    (stmt, p, i) -> stmt.setTimestamp(i, (Timestamp) p),
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP, io.github.hacihaciyev.types.SQLType.DATETIME, io.github.hacihaciyev.types.SQLType.SMALLDATETIME,
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP_WITHOUT_TIME_ZONE
            );

        if (type == LocalDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP, io.github.hacihaciyev.types.SQLType.DATETIME, io.github.hacihaciyev.types.SQLType.DATETIME2,
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP_WITHOUT_TIME_ZONE
            );

        if (type == LocalDate.class)
            return info(
                    (stmt, p, i) -> stmt.setDate(i, Date.valueOf((LocalDate) p)),
                    io.github.hacihaciyev.types.SQLType.DATE
            );

        if (type == LocalTime.class)
            return info(
                    (stmt, p, i) -> stmt.setTime(i, Time.valueOf((LocalTime) p)),
                    io.github.hacihaciyev.types.SQLType.TIME
            );

        if (type == Instant.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p, JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP_WITH_TIME_ZONE, io.github.hacihaciyev.types.SQLType.DATETIMEOFFSET
            );

        if (type == OffsetDateTime.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p, JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP_WITH_TIME_ZONE, io.github.hacihaciyev.types.SQLType.DATETIMEOFFSET
            );

        if (type == ZonedDateTime.class)
            return info(
                    (stmt, p, i) ->
                            stmt.setObject(i, ((ZonedDateTime) p).toOffsetDateTime(), JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    io.github.hacihaciyev.types.SQLType.TIMESTAMP_WITH_TIME_ZONE, io.github.hacihaciyev.types.SQLType.DATETIMEOFFSET
            );

        if (type == Time.class)
            return info(
                    (stmt, p, i) -> stmt.setTime(i, (Time) p),
                    io.github.hacihaciyev.types.SQLType.TIME
            );

        if (type == Date.class)
            return info(
                    (stmt, p, i) -> stmt.setDate(i, (Date) p),
                    io.github.hacihaciyev.types.SQLType.DATE
            );

        if (type == Duration.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    io.github.hacihaciyev.types.SQLType.INTERVAL
            );

        if (type == Period.class)
            return info(
                    (stmt, p, i) -> stmt.setObject(i, p),
                    io.github.hacihaciyev.types.SQLType.INTERVAL
            );

        if (type == Year.class)
            return info(
                    (stmt, p, i) -> stmt.setInt(i, ((Year) p).getValue()),
                    io.github.hacihaciyev.types.SQLType.YEAR, io.github.hacihaciyev.types.SQLType.INT, io.github.hacihaciyev.types.SQLType.SMALLINT
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
                    io.github.hacihaciyev.types.SQLType.NULL, io.github.hacihaciyev.types.SQLType.CURSOR, io.github.hacihaciyev.types.SQLType.TABLE_TYPE
            );

        return TypeInfo.NONE;
    }

    private static TypeInfo info(Setter setter, io.github.hacihaciyev.types.SQLType... sqlTypes) {
        return new TypeInfo.Some(setter, Set.of(sqlTypes));
    }

    private static io.github.hacihaciyev.types.SQLType[] charseqtypes() {
        return new io.github.hacihaciyev.types.SQLType[]{
                io.github.hacihaciyev.types.SQLType.VARCHAR, io.github.hacihaciyev.types.SQLType.TEXT, io.github.hacihaciyev.types.SQLType.CHAR, io.github.hacihaciyev.types.SQLType.CHARACTER,
                io.github.hacihaciyev.types.SQLType.NCHAR, io.github.hacihaciyev.types.SQLType.NVARCHAR, io.github.hacihaciyev.types.SQLType.CHARACTER_VARYING,
                io.github.hacihaciyev.types.SQLType.NATIONAL_CHAR, io.github.hacihaciyev.types.SQLType.NATIONAL_CHAR_VARYING,
                io.github.hacihaciyev.types.SQLType.XML, io.github.hacihaciyev.types.SQLType.JSON, io.github.hacihaciyev.types.SQLType.JSONB, io.github.hacihaciyev.types.SQLType.HIERARCHYID
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

    private static <T> TypeInfo singleValueRecord(TypeMeta.Record<T> rec) {
        if (rec.fields().length != 1) return TypeInfo.NONE;

        var field = rec.fields()[0];

        var fieldInfo = standardTypes(field.type());
        if (!(fieldInfo instanceof TypeInfo.Some(Setter setter, Set<SQLType> sqlTypes)))
            return TypeInfo.NONE;

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

        return new TypeInfo.WithFactory<>(recordSetter, sqlTypes, rec.fields(), rec.factory());
    }
}
