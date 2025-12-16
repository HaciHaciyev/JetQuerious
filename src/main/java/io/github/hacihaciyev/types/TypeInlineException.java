package io.github.hacihaciyev.types;

import java.lang.reflect.RecordComponent;
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
    private final String componentName;

    public TypeInlineException(Class<?> recordType, RecordComponent component, Throwable cause) {
        super(errorMessage(recordType, component), cause);

        Objects.requireNonNull(recordType);
        Objects.requireNonNull(component);
        Objects.requireNonNull(cause);

        this.recordTypeName = recordType.getName();
        this.componentName = component.getName();
    }

    private static String errorMessage(Class<?> recordType, RecordComponent component) {
        return String.format(
                "Unable to map single-component record '%s', component '%s'.", recordType.getName(), component.getName()
        );
    }

    public String recordTypeName() {
        return recordTypeName;
    }

    public String componentName() {
        return componentName;
    }
}
