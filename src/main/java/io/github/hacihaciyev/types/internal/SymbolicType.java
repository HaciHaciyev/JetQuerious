package io.github.hacihaciyev.types.internal;

import java.lang.constant.ClassDesc;
import java.util.OptionalInt;

public sealed interface SymbolicType {
    Unknown UNKNOWN = new Unknown();

    ClassDesc type();

    record Known(ClassDesc type) implements SymbolicType {}

    record KnownInt(ClassDesc type, int value) implements SymbolicType {}

    record FromField(ClassDesc owner, String fieldName, ClassDesc type) implements SymbolicType {}

    record ArrayBuild(ClassDesc elementType, SymbolicType[] elements) implements SymbolicType {
        
        @Override
        public ClassDesc type() {
            return elementType.arrayType();
        }
    }

    record Deconstructed(ClassDesc recordType, OptionalInt limit) implements SymbolicType {

        @Override
        public ClassDesc type() {
            return ClassDesc.of("io.github.hacihaciyev.jdbc.Deconstruction");
        }
    }

    record Unknown() implements SymbolicType {
        
        @Override
        public ClassDesc type() {
            return ClassDesc.of("java.lang.Object");
        }
    }
}