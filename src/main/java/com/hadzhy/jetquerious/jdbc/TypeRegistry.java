package com.hadzhy.jetquerious.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
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

public class TypeRegistry {

    static final Class<?>[] SUPPORTED_CLASSES = supportedClasses();
    static final long TYPE_MASK;
    static {
        long mask = 0;
        for (Class<?> clazz : SUPPORTED_CLASSES) {
            int hash = System.identityHashCode(clazz);
            mask |= (1L << (hash & 0x3F));
        }
        TYPE_MASK = mask;
    }

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

    private TypeRegistry() {}

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
        if (param == null) return true;
        if (isSupportedSimpleType(param)) return true;
        return isSupportedValueObjectType(param);
    }

    private static boolean isSupportedSimpleType(Object param) {
        Class<?> clazz = param.getClass();
        int hash = System.identityHashCode(clazz);
        boolean typeNotSupported = (TYPE_MASK & (1L << (hash & 0x3F))) == 0;
        if (typeNotSupported) return param instanceof Enum<?>;
        return true;
    }

    private static boolean isSupportedValueObjectType(Object param) {
        Class<?> aClass = param.getClass();

        if (TypeRegistry.FIELDS.containsKey(aClass)) return true;

        Field[] fields = getDeclaredInstanceFields(aClass);
        if (fields.length != 1) return false;
        Field field = fields[0];

        try {
            field.setAccessible(true);

            Object value = field.get(param);
            boolean supportedType = isSupportedSimpleType(value);
            if (!supportedType) return false;

            TypeRegistry.FIELDS.put(aClass, field);
            return true;
        } catch (IllegalAccessException | NullPointerException | InaccessibleObjectException | SecurityException e) {
            return false;
        }
    }

    private static Field[] getDeclaredInstanceFields(Class<?> clazz) {
        Field[] allFields = clazz.getDeclaredFields();
        return Arrays.stream(allFields)
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .toArray(Field[]::new);
    }

    private static Class<?>[] supportedClasses() {
        return new Class[]{
                String.class,
                StringBuilder.class,
                StringBuffer.class,
                CharSequence.class,
                Byte.class,
                Integer.class,
                Short.class,
                Long.class,
                Float.class,
                Double.class,
                Boolean.class,
                Character.class,
                UUID.class,
                Time.class,
                Timestamp.class,
                LocalDateTime.class,
                LocalDate.class,
                LocalTime.class,
                Instant.class,
                ZonedDateTime.class,
                OffsetDateTime.class,
                Duration.class,
                Period.class,
                Year.class,
                YearMonth.class,
                MonthDay.class,
                BigDecimal.class,
                BigInteger.class,
                AtomicInteger.class,
                AtomicLong.class,
                AtomicBoolean.class,
                URL.class,
                URI.class,
                Path.class,
                Blob.class,
                Clob.class,
                byte[].class
        };
    }
}
