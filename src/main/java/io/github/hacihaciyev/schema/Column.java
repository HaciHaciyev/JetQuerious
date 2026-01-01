package io.github.hacihaciyev.schema;

public sealed interface Column permits KnownBase, KnownAlias, UnknownBase, UnknownAlias {
    String name();
    boolean known();
    boolean nullable();
}
