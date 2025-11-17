package io.github.hacihaciyev.types;

public enum ArrayDefinition {
    TEXT(String.class),
    VARCHAR(String.class),
    INT(Integer.class),
    INTEGER(Integer.class),
    BIGINT(Long.class),
    BOOLEAN(Boolean.class),
    UUID(java.util.UUID.class),
    DATE(java.sql.Date.class),
    TIMESTAMP(java.sql.Timestamp.class);

    private final Class<?> typeClass;

    ArrayDefinition(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public Class<?> typeClass() {
        return typeClass;
    }

    public static ArrayDefinition from(String name) {
        return switch (name.toLowerCase()) {
            case "text" -> TEXT;
            case "varchar" -> VARCHAR;
            case "int" -> INT;
            case "integer" -> INTEGER;
            case "bigint" -> BIGINT;
            case "boolean" -> BOOLEAN;
            case "uuid" -> UUID;
            case "date" -> DATE;
            case "timestamp" -> TIMESTAMP;
            default -> throw new IllegalArgumentException("Unsupported type: " + name);
        };
    }
}
