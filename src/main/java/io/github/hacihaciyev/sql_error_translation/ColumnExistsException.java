package io.github.hacihaciyev.sql_error_translation;

public class ColumnExistsException extends RepositoryException {
    public ColumnExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
