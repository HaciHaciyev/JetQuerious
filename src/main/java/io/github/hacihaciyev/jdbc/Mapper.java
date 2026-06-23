package io.github.hacihaciyev.jdbc;

public final class Mapper {

    private Mapper() {}

    public static <T> MapperBuilder<T> declare(Class<T> type) {
        return MapperBuilder.declare(type);
    }

    public static <T> MapperBuilder<T> namingConventions(Class<T> type) {
        return MapperBuilder.namingConventions(type);
    }
}