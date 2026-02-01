package io.github.hacihaciyev.types;

import java.util.UUID;

public sealed interface UUIDStrategy {
    UUID value();

    record Native(UUID value) implements UUIDStrategy {}

    record Charseq(UUID value) implements UUIDStrategy {}

    record Binary(UUID value) implements UUIDStrategy {}

    enum Type {
        NATIVE,
        CHARSEQ,
        BINARY;

        public UUIDStrategy create(UUID value) {
            return switch (this) {
                case NATIVE  -> new UUIDStrategy.Native(value);
                case CHARSEQ -> new UUIDStrategy.Charseq(value);
                case BINARY  -> new UUIDStrategy.Binary(value);
            };
        }
    }
}
