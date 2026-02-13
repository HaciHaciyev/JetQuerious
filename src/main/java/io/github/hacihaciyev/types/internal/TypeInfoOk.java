package io.github.hacihaciyev.types.internal;

import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.types.Setter;

import java.util.Set;

public sealed interface TypeInfoOk permits TypeInfo.Some, TypeInfo.WithFactory {
    Setter setter();

    Set<SQLType> sqlTypes();
}
