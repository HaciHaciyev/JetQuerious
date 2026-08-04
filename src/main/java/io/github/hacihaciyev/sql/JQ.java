package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.DML;
import io.github.hacihaciyev.sql.internal.DQL;
import io.github.hacihaciyev.types.internal.BuildTimeRegistry;

import static java.util.Objects.requireNonNull;

public sealed interface JQ {
    String sql();
    
    record Read(String sql, DQL context) implements JQ {
        public Read {
            requireNonNull(sql);
            requireNonNull(context);

            BuildTimeRegistry.register((Context) context);
        }
    }
    
    record Write(String sql, DML context) implements JQ {
        public Write {
            requireNonNull(sql);
            requireNonNull(context);

            BuildTimeRegistry.register((Context) context);
        }
    }
}