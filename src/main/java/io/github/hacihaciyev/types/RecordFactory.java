package io.github.hacihaciyev.types;

import io.github.hacihaciyev.util.Result;

@FunctionalInterface
public interface RecordFactory<T> {
    Result<T, Exception> create(Object... args);
}
