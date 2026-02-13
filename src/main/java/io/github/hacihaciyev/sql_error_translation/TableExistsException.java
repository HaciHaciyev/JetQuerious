package io.github.hacihaciyev.sql_error_translation;

public class TableExistsException extends RepositoryException {
    public TableExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
