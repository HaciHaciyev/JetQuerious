package io.github.hacihaciyev.sql;

/**
 * Exception thrown when schema verification fails during build-time validation.
 *
 * <p>Schema verification ensures that user-specified tables and columns exist in the database
 * and match expected types before runtime execution.
 */
public class SchemaVerificationException extends Exception {
    public SchemaVerificationException(String message) {
        super(message);
    }

    public SchemaVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaVerificationException(Throwable cause) {
        super(cause);
    }
}
