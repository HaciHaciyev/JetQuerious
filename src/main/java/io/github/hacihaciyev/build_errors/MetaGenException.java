package io.github.hacihaciyev.build_errors;

/**
 * This exception indicates that there are problems with code generation,
 * which may be due to invalid package specifications or other
 */
public final class MetaGenException extends JetQueriousBuildException {

    public MetaGenException(String message) {
        super(message);
    }

    public MetaGenException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetaGenException(Throwable cause) {
        super(cause);
    }
}
