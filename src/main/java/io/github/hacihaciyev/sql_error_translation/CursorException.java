package io.github.hacihaciyev.sql_error_translation;

public class CursorException extends RepositoryException {
    public CursorException(String errorCode, String message) {
        super(errorCode, message);
    }
}
