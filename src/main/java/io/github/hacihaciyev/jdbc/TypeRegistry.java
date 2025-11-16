package io.github.hacihaciyev.jdbc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.concurrent.ConcurrentHashMap;

class TypeRegistry {
    static final ConcurrentHashMap<Class<?>, MethodHandle> RECORD_ACCESSORS = new ConcurrentHashMap<>();

    private TypeRegistry() {}

    static void validateArrayElementsMatchDefinition(Object[] array, ArrayDefinition definition) {
        Class<?> expectedType = definition.typeClass();

        for (Object element : array) {
            if (element != null && !expectedType.isAssignableFrom(element.getClass()))
                throw new IllegalArgumentException("Element '%s' does not match expected type %s"
                        .formatted(element, expectedType.getSimpleName()));
        }
    }

    static boolean isSupportedType(final Class<?> type, final ColumnMeta columnMeta) {
        if (columnMeta.type().isSupportedType(type)) return true;

        Object object = recordAccessor(type);
        return columnMeta.type().isSupportedType(object);
    }

    static boolean isSupportedType(final Object param) {
        if (isSupportedSimpleType(param)) return true;

        Object object = recordAccessor(param);
        return isSupportedSimpleType(object);
    }

    private static boolean isSupportedSimpleType(Object param) {
        if (param == null) return true;
        Class<?> aClass = param.getClass();
        if (ParameterSetter.SETTERS.get(aClass) != null) return true;
        return aClass.isEnum();
    }

    private static Object recordAccessor(Object param) {
        Class<?> clazz = param.getClass();
        if (!clazz.isRecord()) return false;

        MethodHandle accessor = RECORD_ACCESSORS.computeIfAbsent(clazz, TypeRegistry::getRecordAccessor);
        if (accessor == null) return false;

        try {
            return accessor.invoke(param);
        } catch (Throwable t) {
            return null;
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
