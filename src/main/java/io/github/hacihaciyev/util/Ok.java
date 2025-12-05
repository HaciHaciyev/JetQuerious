package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public record Ok<T, E extends Exception>(T value) implements Result<T, E> {
    public Ok {
        requireNonNull(value, "Value can`t be null");
    }

    public boolean isOk() {
        return true;
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
    public boolean contains(T value) {
        return this.value.equals(value);
    }

    @Override
    public Optional<T> asOptional() {
        return Optional.of(value);
    }

    @Override
    public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return new Ok<>(mapper.apply(value));
    }

    @Override
    public <F extends Exception> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
        return new Ok<>(value);
    }

    @Override
    public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
        return mapper.apply(value);
    }

    @Override
    public void handle(Consumer<? super T> onOk, Consumer<? super E> onErr) {
        onOk.accept(value);
    }

    @Override
    public T recover(Function<? super E, ? extends T> fallback) {
        return value;
    }

    @Override
    public Result<T, E> ifOk(Consumer<? super T> action) {
        action.accept(value);
        return this;
    }
}
