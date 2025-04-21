package com.hadzhy.jdbclight.jdbc;

import com.hadzhy.jdbclight.exceptions.InvalidArgumentTypeException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class WrappersMapper {

    public static final Set<Class<?>> wrapperTypes = Set.of(
            String.class,
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            UUID.class,
            LocalDate.class,
            LocalTime.class,
            LocalDateTime.class,
            Instant.class,
            byte[].class
    );

    private static final Map<Class<?>, Function<ResultSet, ?>> wrapperMapFunctions = getWrapperMap();

    public static boolean isSupportedWrapperType(Class<?> type) {
        return wrapperTypes.contains(type);
    }

    public static <T> T map(ResultSet rs, Class<T> type) {
        Function<ResultSet, ?> resultSetFunction = wrapperMapFunctions.get(type);
        if (resultSetFunction == null)
            throw new InvalidArgumentTypeException("Unsupported wrapper type.");

        return (T) resultSetFunction.apply(rs);
    }

    /**
     * Returns a map of wrapper types to their corresponding functions for extracting values from a {@code ResultSet}.
     *
     * @return a map of wrapper types to functions
     */
    private static Map<Class<?>, Function<ResultSet, ?>> getWrapperMap() {
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
                        return ts != null ? ts.toLocalDateTime() : null;
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
                Map.entry(byte[].class, rs -> {
                    try {
                        return rs.getBytes(1);
                    } catch (SQLException e) {
                        throw new InvalidArgumentTypeException("Can’t cast to byte[]: " + e.getMessage());
                    }
                })
        );
    }
}
