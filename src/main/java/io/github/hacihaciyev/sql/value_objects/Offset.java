package io.github.hacihaciyev.sql.value_objects;

public record Offset(int value) {
    public Offset {
        if (value < 0) throw new IllegalArgumentException("Offset cannot be below zero");
    }
}