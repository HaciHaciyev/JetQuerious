package io.github.hacihaciyev.types;

public final class MetaRegistry {

    private MetaRegistry() {}

    public sealed interface TypeMeta {
        None NONE = new None();

        record Record<T>(Class<T> type, Field<T, ?>[] fields, RecordFactory<T> factory) implements TypeMeta {}

        record None() implements TypeMeta {}
    }

    public static TypeMeta meta(Class<?> type) {

        return TypeMeta.NONE;
    }
}
