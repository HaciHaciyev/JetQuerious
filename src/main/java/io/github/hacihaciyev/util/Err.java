package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public record Err<T, E extends Exception>(E err) implements Result<T, E> {
    public Err {
        requireNonNull("Error can`t be null");
    }

    public boolean isOk() {
        return false;
    }
    public boolean isErr() {
        return true;
    }

    @Override
    public T or(T defaultValue) {
        return defaultValue;
    }

    @Override
    public T or(Supplier<T> defaultValue) {
        return defaultValue.get();
    }

    @Override
    public Optional<T> asOptional() {
        return Optional.empty();
    }

    @Override
    public Optional<E> errOptional() {
        return Optional.of(err);
    }

    @Override
    public void throwErr() throws E {
        throw err;
    }

    @Override
    public void throwErr(Supplier<E> error) throws E {
        throw err;
    }

    @Override
    public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return new Err<>(err);
    }

    @Override
    public <F extends Exception> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
        return new Err<>(mapper.apply(err));
    }

}
