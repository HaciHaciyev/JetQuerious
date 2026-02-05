package io.github.hacihaciyev.types;

import java.util.Set;

public sealed interface TypeInfoOk permits TypeInfo.Some, TypeInfo.WithFactory {
    Setter setter();

    Set<SQLType> sqlTypes();
}
