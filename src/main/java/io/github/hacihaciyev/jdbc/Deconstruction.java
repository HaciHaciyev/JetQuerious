package io.github.hacihaciyev.jdbc;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

import io.github.hacihaciyev.types.internal.MetaRegistry;
import io.github.hacihaciyev.types.internal.TypeMeta;

public record Deconstruction(Record record, DeconstructionLimit limit) {

    sealed interface DeconstructionLimit {
        record All() implements DeconstructionLimit {}

        record Specified(int limit) implements DeconstructionLimit {
            public Specified {
                if (limit < 1)
                    throw new IllegalArgumentException("limit must be > 0");
            }
        }
    }

    public Deconstruction {
        requireNonNull(record);
        requireNonNull(limit);
    }

    public static Deconstruction dec(Record record) {
        return new Deconstruction(record, new DeconstructionLimit.All());
    }

    public static Deconstruction dec(Record record, int limit) {
        return new Deconstruction(record, new DeconstructionLimit.Specified(limit));
    }

    public Object[] deconstruct() {
        var meta = MetaRegistry.meta(record.getClass());

        if (!(meta instanceof TypeMeta.Record<?> rec)) throw new IllegalArgumentException("Unsupported record type: " + record.getClass());

        var fields = rec.fields();

        int count = switch (limit) {
            case DeconstructionLimit.All _ -> fields.length;
            case DeconstructionLimit.Specified(var n) -> {
                if (n > fields.length) throw new IllegalArgumentException("Requested " + n + " fields but record contains only " + fields.length);
                yield n;
            }
        };

        var values = new Object[count];
        for (int i = 0; i < count; i++) values[i] = unsafeAccessor(fields[i].accessor()).apply(record);
        
        return values;
    }

    @SuppressWarnings("unchecked")
    private static Function<Object, Object> unsafeAccessor(Function<?, ?> fn) {
        return (Function<Object, Object>) fn;
    }
}