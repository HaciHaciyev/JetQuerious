package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T, E extends Exception> permits Ok, Err {
    boolean isOk();
    boolean isErr();

    T or(T defaultValue);
    T or(Supplier<T> defaultValue);

    boolean contains(T value);
    boolean containsErr(E error);

    Optional<T> asOptional();
    Optional<E> errOptional();

    default void throwErr() throws E {}
    default void throwErr(Supplier<E> error) throws E {}

    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);
    <F extends Exception> Result<T, F> mapErr(Function<? super E, ? extends F> mapper);
    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper);


}
