package io.github.hacihaciyev.sql_error_translation;

public class OperationCanceledException extends RepositoryException {
    public OperationCanceledException(String errorCode, String message) {
        super(errorCode, message);
    }
}
