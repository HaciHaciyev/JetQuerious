package io.github.hacihaciyev.sql_error_translation;

public class InvalidArgumentException extends RepositoryException {
    public InvalidArgumentException(String errorCode, String message) {
        super(errorCode, message);
    }
}
