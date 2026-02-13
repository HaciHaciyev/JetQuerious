package io.github.hacihaciyev.sql_error_translation;

public class NullConstraintException extends RepositoryException {
    public NullConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
