package io.github.hacihaciyev.sql_error_translation;

public class ColumnNotFoundException extends RepositoryException {
    public ColumnNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
