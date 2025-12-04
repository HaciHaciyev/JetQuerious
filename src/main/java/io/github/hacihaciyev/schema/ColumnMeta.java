package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.types.SQLType;

public record ColumnMeta(String name, SQLType type, boolean nullable) {

}
