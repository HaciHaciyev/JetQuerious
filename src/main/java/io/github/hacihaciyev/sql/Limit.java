package io.github.hacihaciyev.sql;

public record Limit(int value) {
    public Limit {
        if (value <= 0) throw new IllegalArgumentException("Limit cannot be equals or below zero");
    }
}