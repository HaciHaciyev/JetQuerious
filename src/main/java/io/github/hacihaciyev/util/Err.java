package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public record Err<T, E extends Exception>(E err) implements Result<T, E> {
    public Err {
        requireNonNull("Error can`t be null");
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
    public boolean containsErr(E error) {
        return this.err.equals(error);
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

    @Override
    public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
        return new Err<>(err);
    }

    @Override
    public void handle(Consumer<? super T> onOk, Consumer<? super E> onErr) {
        onErr.accept(err);
    }

    @Override
    public T recover(Function<? super E, ? extends T> fallback) {
        return fallback.apply(err);
    }

    @Override
    public Result<T, E> recoverWith(Function<? super E, ? extends Result<T, E>> fallback) {
        return fallback.apply(err);
    }
}
