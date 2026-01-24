package io.github.hacihaciyev.types;

import java.util.function.Function;

public final class MetaRegistry {

    private MetaRegistry() {}

    public sealed interface TypeMeta {
        None NONE = new None();

        record Record<T>(Class<T> type, Field<T, ?>[] fields) implements TypeMeta {}

        record None() implements TypeMeta {}
    }

    public record Field<T, V>(String name, Class<V> type, Function<T, V> accessor) {}

    public static TypeMeta meta(Class<?> type) {

        return TypeMeta.NONE;
    }
}
