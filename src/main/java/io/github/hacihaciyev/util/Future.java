package io.github.hacihaciyev.util;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Future<T, E extends Exception> implements Result<T, E> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Result<T, E>> ref = new AtomicReference<>();

    public void completeOk(T value) {
        complete(new Ok<>(value));
    }

    public void completeErr(E error) {
        complete(new Err<>(error));
    }

    private void complete(Result<T, E> r) {
        if (!ref.compareAndSet(null, r)) return;
        latch.countDown();
    }

    @Override
    public Result<T, E> await() {
        Result<T, E> r = ref.get();
        if (r != null) return r;
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Err<>((E) ex);
        }
        return ref.get();
    }

    @Override
    public boolean isOk() {
        return await().isOk();
    }

    @Override
    public boolean isErr() {
        return await().isErr();
    }

    @Override
    public T or(T def) {
        return await().or(def);
    }

    @Override
    public T or(Supplier<T> def) {
        return await().or(def);
    }

    @Override
    public boolean contains(T v) {
        return await().contains(v);
    }

    @Override
    public boolean containsErr(E e) {
        return await().containsErr(e);
    }

    @Override
    public Optional<T> asOptional() {
        return await().asOptional();
    }

    @Override
    public Optional<E> errOptional() {
        return await().errOptional();
    }

    @Override
    public void throwErr() throws E {
        await().throwErr();
    }

    @Override
    public void throwErr(Supplier<E> e) throws E {
        await().throwErr(e);
    }

    @Override
    public <U> Result<U, E> map(Function<? super T, ? extends U> f) {
        return await().map(f);
    }

    @Override
    public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> f) {
        return await().flatMap(f);
    }

    @Override
    public <F extends Exception> Result<T, F> mapErr(Function<? super E, ? extends F> f) {
        return await().mapErr(f);
    }

    @Override
    public void handle(Consumer<? super T> ok, Consumer<? super E> err) {
        await().handle(ok, err);
    }

    @Override
    public <U> U fold(Function<? super T, ? extends U> o, Function<? super E, ?extends U> e) {
        return await().fold(o, e);
    }

    @Override
    public T recover(Function<? super E, ? extends T> fb) {
        return await().recover(fb);
    }

    @Override
    public Result<T, E> recoverWith(Function<? super E, ? extends Result<T, E>> fb) {
        return await().recoverWith(fb);
    }

    @Override
    public Result<T, E> ifOk(Consumer<? super T> a) {
        return await().ifOk(a);
    }

    @Override
    public Result<T, E> ifErr(Consumer<? super E> a) {
        return await().ifErr(a);
    }

    @Override
    public <U> Result<U, E> and(Result<U, E> next) {
        return await().and(next);
    }

    @Override
    public boolean guardErr(Runnable a) {
        return await().guardErr(a);
    }
}
