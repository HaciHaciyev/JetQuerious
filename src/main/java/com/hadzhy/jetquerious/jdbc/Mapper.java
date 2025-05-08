package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.InvalidArgumentTypeException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class Mapper {

    private static final Map<Class<?>, Function<ResultSet, ?>> typeMappers = getTypesMapper();

    public static <T> T map(ResultSet rs, Class<T> type) {
        Function<ResultSet, ?> resultSetFunction = typeMappers.get(type);
        if (resultSetFunction == null) throw new InvalidArgumentTypeException("Unsupported wrapper type.");
        return (T) resultSetFunction.apply(rs);
    }

    private static Map<Class<?>, Function<ResultSet, ?>> getTypesMapper() {
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
