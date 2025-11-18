package io.github.hacihaciyev.jdbc;

@FunctionalInterface
public interface TransactionContext<T> {
    void accept(T t) throws Exception;
}
