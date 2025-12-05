package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T, E extends Exception> permits Ok, Err {
    default boolean isOk() { return false; }
    default boolean isErr() { return false; }

    T or(T defaultValue);
    T or(Supplier<T> defaultValue);

    default boolean contains(T value) { return false; }
    default boolean containsErr(E error) { return false; }

    default Optional<T> asOptional() { return Optional.empty(); }
    default Optional<E> errOptional() { return Optional.empty(); }

    default void throwErr() throws E {}
    default void throwErr(Supplier<E> error) throws E {}

    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);
    <F extends Exception> Result<T, F> mapErr(Function<? super E, ? extends F> mapper);
    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper);

    void handle(Consumer<? super T> onOk, Consumer<? super E> onErr);
    <U> U fold(Function<? super T, ? extends U> okMapper, Function<? super E, ? extends U> errMapper);

    T recover(Function<? super E, ? extends T> fallback);
    default Result<T, E> recoverWith(Function<? super E, ? extends Result<T, E>> fallback) { return this; }

    default Result<T, E> ifOk(Consumer<? super T> action) { return this; }
    default Result<T, E> ifErr(Consumer<? super E> action) { return this; }

    default <U> Result<U, E> and(Result<U, E> next) {
        return isOk() ? next : (Result<U, E>) this;
    }

    default boolean guardErr(Runnable action) {
        if (isErr()) {
            action.run();
            return true;
        }
        return false;
    }
}
