package io.github.hacihaciyev.jdbc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypeRegistry {

    static final ConcurrentHashMap<Class<?>, MethodHandle> RECORD_ACCESSORS = new ConcurrentHashMap<>();

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

    static boolean isSupportedType(final Object param) {
        if (isSupportedSimpleType(param)) return true;
        return isSupportedValueObjectType(param);
    }

    private static boolean isSupportedSimpleType(Object param) {
        if (param == null) return true;
        Class<?> aClass = param.getClass();
        if (ParameterSetter.SETTERS.get(aClass) != null) return true;
        return aClass.isEnum();
    }

    private static boolean isSupportedValueObjectType(Object param) {
        Class<?> clazz = param.getClass();

        if (!clazz.isRecord()) return false;

        MethodHandle accessor = RECORD_ACCESSORS.computeIfAbsent(clazz, TypeRegistry::getRecordAccessor);
        if (accessor == null) return false;

        try {
            Object value = accessor.invoke(param);
            return isSupportedSimpleType(value);
        } catch (Throwable t) {
            return false;
        }
    }

    private static MethodHandle getRecordAccessor(Class<?> recordClass) {
        return RECORD_ACCESSORS.computeIfAbsent(recordClass, cls -> {
            try {
                RecordComponent[] comps = cls.getRecordComponents();
                if (comps == null || comps.length != 1) return null;
                return MethodHandles.lookup().unreflect(comps[0].getAccessor());
            } catch (IllegalAccessException e) {
                return null;
            }
        });
    }
}
