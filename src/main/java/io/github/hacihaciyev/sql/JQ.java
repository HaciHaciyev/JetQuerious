package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.sql.internal.value_objects.DML;
import io.github.hacihaciyev.sql.internal.value_objects.DQL;

import static java.util.Objects.requireNonNull;

public sealed interface JQ {
    String sql();
    
    record Read(String sql, DQL context) implements JQ {
        public Read {
            requireNonNull(sql);
            requireNonNull(context);
        }
    }
    
    record Write(String sql, DML context) implements JQ {
        public Write {
            requireNonNull(sql);
            requireNonNull(context);
        }
    }
}