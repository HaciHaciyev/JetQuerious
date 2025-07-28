package com.hadzhy.jetquerious.reactive;

public interface Unum<T> {

    OnItem<T> onItem();

    OnFailure<T> onFailure();

    void subscribe(Callback<T> callback);

    Cancellation subscribe(Consumer<? super T> onSuccess, Consumer<Throwable> onFailure);

    /** Завершить ленивое выполнение (если оно не запущено) */
    boolean cancel();

    boolean isCancelled();

    boolean isTerminated();

    /** Позволяет задать кастомный Executor для выполнения подписки */
    Unum<T> runOn(Executor executor);

    /** Создание */
    static <T> Unum<T> from(Callable<T> supplier) {
        return new DeferredUnum<>(supplier);
    }

    static <T> Unum<T> just(T value) {
        return new ImmediateUnum<>(value);
    }

    static <T> Unum<T> failure(Throwable throwable) {
        return new ImmediateUnum<>(throwable);
    }
}
