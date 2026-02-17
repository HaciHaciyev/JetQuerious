package io.github.hacihaciyev.build_errors;

/**
 * Exception thrown when schema verification error or types mismatch happens during build-time validation.
 *
 * <p>Schema verification ensures that user-specified tables and columns exist in the database
 * and match expected types before runtime execution.
 * <p> Type verification ensures that defined Java type and SQL type can be interconverted.
 */
public final class SchemaVerificationException extends JetQueriousBuildException {
    
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
