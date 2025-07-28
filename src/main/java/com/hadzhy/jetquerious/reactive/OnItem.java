package com.hadzhy.jetquerious.reactive;

interface OnItem<T> {
    <U> Unum<U> transform(Function<? super T, ? extends U> mapper);

    <U> Unum<U> transformToUnum(Function<? super T, ? extends Unum<? extends U>> mapper);

    Unum<T> invoke(Consumer<? super T> consumer);
}
