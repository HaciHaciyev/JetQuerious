package io.github.hacihaciyev.sql_error_translation;

public class IncompleteStatementException extends RepositoryException {
    public IncompleteStatementException(String errorCode, String message) {
        super(errorCode, message);
    }
}
