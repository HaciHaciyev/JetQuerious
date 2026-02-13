package io.github.hacihaciyev.types.internal;

public sealed interface TypeMeta {
    None NONE = new None();

    record Record<T>(Class<T> type, Field<T, ?>[] fields, RecordFactory<T> factory) implements TypeMeta {}

    record None() implements TypeMeta {}
}
