package io.github.hacihaciyev.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class MetaRegistry {
    private static final List<String> packages = new ArrayList<>();
    static {
        String pkgs = System.getProperty("jetquerious.packages");
        if (pkgs != null)
            packages.addAll(Arrays.asList(pkgs.split(";")));
    }

    private MetaRegistry() {}

    public sealed interface TypeMeta {
        None NONE = new None();

        record Record<T>(Class<T> type, Field<T, ?>[] fields) implements TypeMeta {}

        record None() implements TypeMeta {}
    }

    public record Field<T, V>(String name, Class<V> type, Function<T, V> accessor) {}

    public static TypeMeta meta(Class<?> type) {

        return TypeMeta.NONE;
    }
}
