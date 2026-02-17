package io.github.hacihaciyev.types;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

public sealed interface UUIDStrategy {
    UUID value();

    record Native(UUID value) implements UUIDStrategy {
        public Native {
            requireNonNull(value, "UUID cannot be null for UUIDStrategy");
        }
    }

    record Charseq(UUID value) implements UUIDStrategy {
        public Charseq {
            requireNonNull(value, "UUID cannot be null for UUIDStrategy");
        }
    }

    record Binary(UUID value) implements UUIDStrategy {
        public Binary {
            requireNonNull(value, "UUID cannot be null for UUIDStrategy");
        }
    }

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
