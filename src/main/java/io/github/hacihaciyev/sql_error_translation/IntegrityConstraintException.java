package io.github.hacihaciyev.sql_error_translation;

public class IntegrityConstraintException extends RepositoryException {
    public IntegrityConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
