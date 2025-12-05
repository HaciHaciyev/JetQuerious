package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public record Ok<T, E extends Exception>(T value) implements Result<T, E> {
    public Ok {
        requireNonNull(value, "Value can`t be null");
    }

    public boolean isOk() {
        return true;
    }
    public boolean isErr() {
        return false;
    }

    @Override
    public T or(T defaultValue) {
        return value;
    }

    @Override
    public T or(Supplier<T> defaultValue) {
        return value;
    }

    @Override
    public Optional<T> asOptional() {
        return Optional.of(value);
    }

    @Override
    public Optional<E> errOptional() {
        return Optional.empty();
    }


}
