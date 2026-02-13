package io.github.hacihaciyev.sql_error_translation;

public class DynamicSQLException extends RepositoryException {
    public DynamicSQLException(String errorCode, String message) {
        super(errorCode, message);
    }
}
