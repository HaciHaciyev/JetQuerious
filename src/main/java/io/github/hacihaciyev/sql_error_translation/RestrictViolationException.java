package io.github.hacihaciyev.sql_error_translation;

public class RestrictViolationException extends RepositoryException {
    public RestrictViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
