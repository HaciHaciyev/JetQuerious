package io.github.hacihaciyev.sql_error_translation;

public class TableNotFoundException extends RepositoryException {
    public TableNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
