package io.github.hacihaciyev.jdbc;

@FunctionalInterface
public interface SQLConsumer<T> {
    void accept(T t) throws Exception;
}
