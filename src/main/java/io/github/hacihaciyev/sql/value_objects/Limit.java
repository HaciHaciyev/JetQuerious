package io.github.hacihaciyev.sql.value_objects;

public record Limit(int value) {
    public Limit {
        if (value <= 0) throw new IllegalArgumentException("Limit cannot be equals or below zero");
    }
}