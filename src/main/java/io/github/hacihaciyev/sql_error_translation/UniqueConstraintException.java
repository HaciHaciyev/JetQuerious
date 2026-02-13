package io.github.hacihaciyev.sql_error_translation;

public class UniqueConstraintException extends RepositoryException {
    public UniqueConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
