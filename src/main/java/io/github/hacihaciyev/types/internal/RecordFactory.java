package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.types.TypeInstantiationException;

@FunctionalInterface
public interface RecordFactory<T> {
    T create(Object... args) throws TypeInstantiationException;
}
