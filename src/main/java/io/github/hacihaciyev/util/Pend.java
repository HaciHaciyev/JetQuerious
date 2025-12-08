package io.github.hacihaciyev.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Pend<T, E extends Exception> {
    private final AtomicReference<Result<T,E>> result = new AtomicReference<>(null);
    private final AtomicReference<Consumer<Result<T,E>>> callback = new AtomicReference<>(null);
    private final AtomicBoolean delivered = new AtomicBoolean(false);

    public void completeOk(T value) {
        complete(new Ok<>(value));
    }

    public void completeErr(E error) {
        complete(new Err<>(error));
    }

    public void onComplete(Consumer<Result<T,E>> cb) {
        Result<T,E> r = result.get();

        if (r != null) {
            if (!callback.compareAndSet(null, cb))
                throw new IllegalStateException("Callback already set");

            deliverOnce(r);
            return;
        }

        if (!callback.compareAndSet(null, cb))
            throw new IllegalStateException("Callback already set");

        r = result.get();
        if (r != null) deliverOnce(r);
    }

    private void complete(Result<T,E> res) {
        if (!result.compareAndSet(null, res)) return;

        Consumer<Result<T,E>> cb = callback.get();
        if (cb != null) deliverOnce(res);
    }

    private void deliverOnce(Result<T,E> res) {
        if (!delivered.compareAndSet(false, true)) return;
        callback.get().accept(res);
        callback.set(null);
    }
}
