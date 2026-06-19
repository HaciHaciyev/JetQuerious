package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.types.Getter;
import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.types.Setter;

import java.util.Arrays;
import java.util.Set;

public sealed interface TypeInfo {
    None NONE = new None();

    record Some(Setter setter, Getter getter, Set<SQLType> sqlTypes) implements TypeInfo, TypeInfoOk {
        public Some {
            sqlTypes = Set.copyOf(sqlTypes);
        }
    }

    record WithFactory<T>(
            Setter setter, Getter getter, Set<SQLType> sqlTypes,
            Field<T, ?>[] fields, RecordFactory<T> factory) implements TypeInfo, TypeInfoOk {

        public WithFactory {
            sqlTypes = Set.copyOf(sqlTypes);
        }

        public Object[] objects(T t) {
            return Arrays.stream(fields).map(f -> f.accessor().apply(t)).toArray();
        }
    }

    record None() implements TypeInfo {}
}