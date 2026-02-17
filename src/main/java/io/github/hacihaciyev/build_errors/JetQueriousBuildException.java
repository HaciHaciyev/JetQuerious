package io.github.hacihaciyev.build_errors;

/**
 * This exception and its descendants are created to indicate that certain errors have occurred
 * related to build-time code generation, 
 * build-time validation, and other operations performed at build time.
 */
public sealed class JetQueriousBuildException extends RuntimeException permits SchemaVerificationException, MetaGenException {

    public JetQueriousBuildException(String message) {
        super(message);
    }

    public JetQueriousBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public JetQueriousBuildException(Throwable cause) {
        super(cause);
    }
}