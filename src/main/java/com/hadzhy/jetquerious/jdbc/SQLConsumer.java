package com.hadzhy.jetquerious.jdbc;

@FunctionalInterface
public interface SQLConsumer<T> {
    void accept(T t) throws Exception;
}
