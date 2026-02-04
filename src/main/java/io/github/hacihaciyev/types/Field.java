package io.github.hacihaciyev.types;

import java.util.function.Function;

public record Field<T, V>(String name, Class<V> type, Function<T, V> accessor) {}
