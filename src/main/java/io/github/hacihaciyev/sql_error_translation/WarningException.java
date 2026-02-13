package io.github.hacihaciyev.sql_error_translation;

public class WarningException extends RepositoryException {
    public WarningException(String errorCode, String message) {
        super(errorCode, message);
    }
}
