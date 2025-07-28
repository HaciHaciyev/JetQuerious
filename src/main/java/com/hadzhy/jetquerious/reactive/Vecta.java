package com.hadzhy.jetquerious.reactive;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Vecta<T> {

    OnItem<T> onItem();

    OnFailure<T> onFailure();

    void subscribe(Subscriber<T> subscriber);

    Cancellation subscribe(Consumer<? super T> onNext,
                          Consumer<Throwable> onFailure,
                          Runnable onComplete);

    boolean cancel();

    boolean isCancelled();

    boolean isTerminated();

    Vecta<T> runOn(Executor executor);

    static <T> Vecta<T> fromIterable(Iterable<T> iterable) {
        return new ImmediateVecta<>(iterable);
    }

    static <T> Vecta<T> failure(Throwable throwable) {
        return new ImmediateVecta<>(throwable);
    }
}
