package io.github.hacihaciyev.types.internal;

import java.util.function.Function;

public record Field<T, V>(String name, Class<V> type, Function<T, V> accessor) {}
