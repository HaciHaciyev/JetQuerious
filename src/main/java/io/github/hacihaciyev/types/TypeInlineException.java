package io.github.hacihaciyev.types;

import java.util.Objects;

/**
 * Exception thrown when a single-field record cannot be mapped
 * into natively supported types.
 *
 * <p>This is a checked exception to explicitly signal that
 * the value was <strong>not</strong> set.</p>
 */
public class TypeInlineException extends Exception {
    private final String recordTypeName;

    public TypeInlineException(Class<?> recordType, Throwable cause) {
        super(errorMessage(recordType), cause);

        Objects.requireNonNull(recordType);
        Objects.requireNonNull(cause);

        this.recordTypeName = recordType.getName();
    }

    private static String errorMessage(Class<?> recordType) {
        return String.format("Unable to map single-component record '%s'.", recordType.getName());
    }

    public String recordTypeName() {
        return recordTypeName;
    }
}
