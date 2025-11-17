package io.github.hacihaciyev.types;

import java.util.UUID;

public sealed interface UUIDStrategy {
    UUID value();

    record Native(UUID value) implements UUIDStrategy {}

    record Charseq(UUID value) implements UUIDStrategy {}

    record Binary(UUID value) implements UUIDStrategy {}
}
