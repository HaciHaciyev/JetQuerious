package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.build_errors.MetaGenException;
import io.github.hacihaciyev.sql.internal.Context;

import java.util.ArrayDeque;
import java.util.Deque;

public final class BuildTimeRegistry implements AutoCloseable {

    private static final ThreadLocal<BuildTimeRegistry> current = new ThreadLocal<>();

    private final Deque<QueryMetadata> queries = new ArrayDeque<>();

    private BuildTimeRegistry() {}

    static BuildTimeRegistry open() {
        if (current.get() != null) {
            throw new IllegalStateException("BuildTimeRegistry already opened");
        }

        var registry = new BuildTimeRegistry();
        current.set(registry);
        return registry;
    }

    public static void register(Context context) {
        var registry = current.get();
        if (registry != null) registry.queries.addLast(QueryMetadata.from(context));
    }

    QueryMetadata next() {
        var metadata = queries.pollFirst();
        if (metadata == null) throw new MetaGenException("JetQuerious. Build-time registry underflow.");
        return metadata;
    }

    boolean isEmpty() {
        return queries.isEmpty();
    }

    int size() {
        return queries.size();
    }

    @Override
    public void close() {
        queries.clear();
        current.remove();
    }
}