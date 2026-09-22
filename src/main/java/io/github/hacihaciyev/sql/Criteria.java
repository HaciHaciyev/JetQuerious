package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.types.internal.BuildTimeRegistry;

import static java.util.Objects.requireNonNull;

public record Criteria(Context.Criteria context) {

    public Criteria {
        requireNonNull(context, "context cannot be null");
        BuildTimeRegistry.register(context);
    }
}