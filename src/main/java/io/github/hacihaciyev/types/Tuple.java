package io.github.hacihaciyev.types;

import java.lang.reflect.RecordComponent;
import static java.util.Objects.requireNonNull;

public sealed interface Tuple {

    int length();

    record Duo(Record value) implements Tuple {
        public Duo {
            validateRecord(value, 2);
        }

        @Override
        public int length() {
            return 2;
        }
    }

    record Tri(Record value) implements Tuple {
        public Tri {
            validateRecord(value, 3);
        }

        @Override
        public int length() {
            return 3;
        }
    }

    record Quad(Record value) implements Tuple {
        public Quad {
            validateRecord(value, 4);
        }

        @Override
        public int length() {
            return 4;
        }
    }

    record Quint(Record value) implements Tuple {
        public Quint {
            validateRecord(value, 5);
        }

        @Override
        public int length() {
            return 5;
        }
    }

    record Sext(Record value) implements Tuple {
        public Sext {
            validateRecord(value, 6);
        }

        @Override
        public int length() {
            return 6;
        }
    }

    record Sept(Record value) implements Tuple {
        public Sept {
            validateRecord(value, 7);
        }

        @Override
        public int length() {
            return 7;
        }
    }

    record Oct(Record value) implements Tuple {
        public Oct {
            validateRecord(value, 8);
        }

        @Override
        public int length() {
            return 8;
        }
    }

    record Non(Record value) implements Tuple {
        public Non {
            validateRecord(value, 9);
        }

        @Override
        public int length() {
            return 9;
        }
    }

    record Dec(Record value) implements Tuple {
        public Dec {
            validateRecord(value, 10);
        }

        @Override
        public int length() {
            return 10;
        }
    }

    private static void validateRecord(Record value, int expectedLength) {
        requireNonNull(value, "Null is not allowed for tuples");

        RecordComponent[] comps = value.getClass().getRecordComponents();
        if (comps.length != expectedLength) throw new IllegalArgumentException(
                "Expected record with " + expectedLength + " components, but got " + comps.length);
    }
}
