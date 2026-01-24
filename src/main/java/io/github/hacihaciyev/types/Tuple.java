package io.github.hacihaciyev.types;

import static io.github.hacihaciyev.types.MetaRegistry.meta;
import static io.github.hacihaciyev.types.MetaRegistry.TypeMeta;
import static java.util.Objects.requireNonNull;

public sealed interface Tuple {

    String UNSUPPORTED_RECORD =
            "Unsupported record type {%s}. If you want to use this record specify it`s package for build time meta data generation.";

    String RECORD_INVALID_LENGTH =
            "Provided record type {%s} doesn`t contains sufficient fields for defined tuple.";

    int length();

    Object[] array();

    static Duo duo(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 2);
        return new Duo(f[0], f[1]);
    }

    static Tri tri(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 3);
        return new Tri(f[0], f[1], f[2]);
    }

    static Quad quad(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 4);
        return new Quad(f[0], f[1], f[2], f[3]);
    }

    static Quint quint(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 5);
        return new Quint(f[0], f[1], f[2], f[3], f[4]);
    }

    static Sext sext(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 6);
        return new Sext(f[0], f[1], f[2], f[3], f[4], f[5]);
    }

    static Sept sept(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 7);
        return new Sept(f[0], f[1], f[2], f[3], f[4], f[5], f[6]);
    }

    static Oct oct(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 8);
        return new Oct(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7]);
    }

    static Non non(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 9);
        return new Non(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]);
    }

    static Dec dec(Record value) {
        requireNonNull(value, "Null is not allowed for tuples");

        var f = extract(value, 10);
        return new Dec(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9]);
    }

    record Duo(Object a, Object b) implements Tuple {
        @Override
        public int length() {
            return 2;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b};
        }
    }

    record Tri(Object a, Object b, Object c) implements Tuple {
        @Override
        public int length() {
            return 3;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c};
        }
    }

    record Quad(Object a, Object b, Object c, Object d) implements Tuple {
        @Override
        public int length() {
            return 4;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d};
        }
    }

    record Quint(Object a, Object b, Object c, Object d, Object e) implements Tuple {
        @Override
        public int length() {
            return 5;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d, e};
        }
    }

    record Sext(Object a, Object b, Object c, Object d, Object e, Object f) implements Tuple {
        @Override
        public int length() {
            return 6;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d, e, f};
        }
    }

    record Sept(Object a, Object b, Object c, Object d, Object e, Object f, Object g) implements Tuple {
        @Override
        public int length() {
            return 7;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d, e, f, g};
        }
    }

    record Oct(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) implements Tuple {
        @Override
        public int length() {
            return 8;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d, e, f, g, h};
        }
    }

    record Non(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) implements Tuple {
        @Override
        public int length() {
            return 9;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d, e, f, g, h, i};
        }
    }

    record Dec(Object a, Object b, Object c, Object d, Object e,
               Object f, Object g, Object h, Object i, Object j) implements Tuple {
        @Override
        public int length() {
            return 10;
        }

        @Override
        public Object[] array() {
            return new Object[]{a, b, c, d, e, f, g, h, i, j};
        }
    }

    private static Object[] extract(Record value, int requiredLength) {
        return switch (meta(value.getClass())) {
            case TypeMeta.Record(_, var fields) -> {
                if (fields.length < requiredLength)
                    throw new IllegalArgumentException(RECORD_INVALID_LENGTH.formatted(value.getClass().getName()));
                yield fields;
            }
            case TypeMeta.None _ -> throw new IllegalArgumentException(UNSUPPORTED_RECORD.formatted(value.getClass().getName()));
        };
    }
}
