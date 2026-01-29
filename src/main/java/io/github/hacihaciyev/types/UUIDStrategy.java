package io.github.hacihaciyev.types;

import java.util.UUID;

import static io.github.hacihaciyev.types.TypeRegistry.UNSUPPORTED_RECORD;
import static java.util.Objects.requireNonNull;

public sealed interface UUIDStrategy {

    String INVALID_UUID_STRATEGY = "UUID strategy is allowed only for UUID and single value records which contains UUID";

    UUID value();

    static Native nativeUUID(Record value) {
        requireNonNull(value, "Null is not allowed for uuid strategy");
        return new Native(extract(value));
    }

    static Charseq charseqUUID(Record value) {
        requireNonNull(value, "Null is not allowed for uuid strategy");
        return new Charseq(extract(value));
    }

    static Binary binaryUUID(Record value) {
        requireNonNull(value, "Null is not allowed for uuid strategy");
        return new Binary(extract(value));
    }

    record Native(UUID value) implements UUIDStrategy {
        public Native {
            requireNonNull(value, "Null is not allowed for uuid strategy");
        }
    }

    record Charseq(UUID value) implements UUIDStrategy {
        public Charseq {
            requireNonNull(value, "Null is not allowed for uuid strategy");
        }
    }

    record Binary(UUID value) implements UUIDStrategy {
        public Binary {
            requireNonNull(value, "Null is not allowed for uuid strategy");
        }
    }

    private static UUID extract(Record value) {
        return switch (MetaRegistry.meta(value.getClass())) {
            case MetaRegistry.TypeMeta.Record(_, var fields, _) -> {
                if (fields.length > 1) throw new IllegalArgumentException(INVALID_UUID_STRATEGY);
                var component = fields[0].accessor().apply(value);
                if (!(component instanceof UUID uuid)) throw new IllegalArgumentException(INVALID_UUID_STRATEGY);
                yield uuid;
            }
            case MetaRegistry.TypeMeta.None _ ->
                    throw new IllegalArgumentException(UNSUPPORTED_RECORD.formatted(value.getClass().getName()));
        };
    }
}
