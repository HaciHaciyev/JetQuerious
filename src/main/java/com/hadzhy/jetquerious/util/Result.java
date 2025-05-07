package com.hadzhy.jetquerious.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Result type is used to encapsulate a result of status or Exception if something goes wrong
 * Used as better alternative for Optionals which can't tell you about what went wrong
 *
 * @param <V>  result of an status
 * @param <E>  Exception that tells what went wrong
 * @param success  allows to pass null at value and throwable
 */
public record Result<V, E extends Throwable>(V value,
                                             E throwable,
                                             boolean success) {

    /**
     * @return returns new Result encapsulating result of an status
     */
    public static <V, E extends Throwable> Result<V, E> success(V value) {
        return new Result<>(value, null, true);
    }

    /**
     * @return returns new Result encapsulating Exception that is thrown by an status
     */
    public static <V, E extends Throwable> Result<V, E> failure(E throwable) {
        return new Result<>(null, throwable, false);
    }

    public static <V, E extends Throwable> Result<V, E> ofThrowable(Supplier<? extends V> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure((E) e);
        }
    }

    /**
     * @return returns mapped value if success and Optional.empty() otherwise or if something went wrong
     */
    public <R> Optional<R> mapSuccess(Function<V, R> mapper) {
        return this.success ? Optional.ofNullable(this.value).map(mapper)
                            : Optional.empty();
    }

    /**
     * @return returns mapped value if failure and Optional.empty() otherwise or if something went wrong
     */
    public <R> Optional<R> mapFailure(Function<E, R> mapper) {
        return this.success ? Optional.empty()
                            : Optional.ofNullable(this.throwable).map(mapper);
    }

    /**
     * @return applies mappers to both success and failure returning a new value
     */
    public <R> R map(Function<V, R> successMapper,
                     Function<E, R> failureMapper) {
        return this.success ? successMapper.apply(this.value)
                            : failureMapper.apply(this.throwable);
    }

    /**
     * side effect only method that applies on case of Success
     */
    public void ifSuccess(Consumer<? super V> action) {
        if (this.success) {
            action.accept(this.value);
        }
    }

    /**
     *  side effect only method that applies on case of Failure
     */
    public void ifFailure(Consumer<? super E> action) {
        if (!this.success) {
            action.accept(this.throwable);
        }
    }

    /**
     * side effect only method that applies either to Success or Failure whichever is present
     */
    public void handle(Consumer<? super V> successAction,
                       Consumer<? super E> failureAction) {
        if (this.success) {
            successAction.accept(this.value);
        } else {
            failureAction.accept(this.throwable);
        }
    }

    /**
     * @return fallback value if something went wrong
     */
    public V orElse(V other) {
        return this.success ? this.value
                            : other;
    }

    /**
     * @return fallback value if something went wrong
     */
    public V orElseGet(Supplier<? extends V> otherSupplier) {
        return this.success ? this.value
                            : otherSupplier.get();
    }


    /**
     * @return tries to return value and throws an Exception if something went wrong
     */
    public V orElseThrow() {
        if (!this.success) {
            sneakyThrow(this.throwable);
            return null;
        }
        return this.value;
    }

    public <X extends Throwable> V orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (!this.success) {
            throw exceptionSupplier.get();
        } else {
            return this.value;
        }
    }

    private <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
