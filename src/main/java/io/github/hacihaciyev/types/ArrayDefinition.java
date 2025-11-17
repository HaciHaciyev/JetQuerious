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
}
