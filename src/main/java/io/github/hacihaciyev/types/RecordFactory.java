package io.github.hacihaciyev.types;

@FunctionalInterface
public interface RecordFactory<T> {
    T create(Object... args) throws TypeInstantiationException;
}
