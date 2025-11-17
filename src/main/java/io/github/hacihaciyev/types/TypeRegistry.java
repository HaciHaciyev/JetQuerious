package io.github.hacihaciyev.types;

import io.github.hacihaciyev.schema.ColumnMeta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TypeRegistry {
    static final ConcurrentMap<Class<?>, MethodHandle> RECORD_ACCESSORS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<?>, Class<?>> RECORD_COMPONENT_TYPES = new ConcurrentHashMap<>();

    private TypeRegistry() {}

    public static void validateArrayElementsMatchDefinition(Object[] array, ArrayDefinition definition) {
        Class<?> expectedType = definition.typeClass();

        for (Object element : array) {
            if (element != null && !expectedType.isAssignableFrom(element.getClass()))
                throw new IllegalArgumentException("Element '%s' does not match expected type %s"
                        .formatted(element, expectedType.getSimpleName()));
        }
    }

    public static boolean isSupportedType(final Class<?> type, final ColumnMeta columnMeta) {
        if (columnMeta.type().isSupportedType(type)) return true;

        Class<?> componentType = singleComponentOfRecord(type);
        if (componentType != null) return columnMeta.type().isSupportedType(componentType);
        return false;
    }

    public static boolean isSupportedType(final Object param) {
        if (isSupportedSimpleType(param)) return true;

        MethodHandle methodHandle = singleRecordMethodHandle(param);
        if (methodHandle == null) return false;

        Object extracted = extractSingleComponent(param, methodHandle);
        if (extracted != null && extracted != param) return isSupportedSimpleType(extracted);

        return false;
    }

    private static boolean isSupportedSimpleType(Object param) {
        if (param == null) return true;
        Class<?> aClass = param.getClass();
        if (ParameterSetter.SETTERS.get(aClass) != null) return true;
        return aClass.isEnum();
    }

    private static Class<?> singleComponentOfRecord(Class<?> clazz) {
        if (!clazz.isRecord()) return null;

        return RECORD_COMPONENT_TYPES.computeIfAbsent(clazz, cls -> {
            RecordComponent[] comps = cls.getRecordComponents();
            if (comps == null || comps.length != 1) return null;

            RECORD_ACCESSORS.computeIfAbsent(cls, TypeRegistry::createRecordAccessor);
            return comps[0].getType();
        });
    }

    private static MethodHandle singleRecordMethodHandle(Object param) {
        if (param == null) return null;

        Class<?> clazz = param.getClass();
        if (!clazz.isRecord()) return null;

        return RECORD_ACCESSORS.computeIfAbsent(clazz, TypeRegistry::createRecordAccessor);
    }

    private static MethodHandle createRecordAccessor(Class<?> recordClass) {
        try {
            RecordComponent[] comps = recordClass.getRecordComponents();
            if (comps == null || comps.length != 1) return null;
            return MethodHandles.lookup().unreflect(comps[0].getAccessor());
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Object extractSingleComponent(Object param, MethodHandle methodHandle) {
        try {
            return methodHandle.invoke(param);
        } catch (Throwable e) {
            return null;
        }
    }
}