package io.github.hacihaciyev.types;

import java.util.Objects;

/**
 * Exception thrown when a record cannot be instantiated via RecordFactory.
 *
 * <p>This is a checked exception to explicitly signal that
 * the record instance was <strong>not</strong> created.</p>
 */
public class TypeInstantiationException extends Exception {
    
    private final String recordTypeName;
    
    public TypeInstantiationException(Class<?> recordType, Throwable cause) {
        super(errorMessage(recordType), cause);
        
        Objects.requireNonNull(recordType);
        Objects.requireNonNull(cause);
        
        this.recordTypeName = recordType.getName();
    }
    
    private static String errorMessage(Class<?> recordType) {
        return String.format("Unable to instantiate record '%s'.", recordType.getName());
    }
    
    public String recordTypeName() {
        return recordTypeName;
    }
}