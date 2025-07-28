package com.hadzhy.jetquerious.reactive;

interface OnFailure<T> {
    Unum<T> invoke(Consumer<Throwable> consumer);

    Unum<T> recoverWith(Function<Throwable, ? extends T> fallback);

    Unum<T> recoverWithUnum(Function<Throwable, ? extends Unum<T>> fallback);
}