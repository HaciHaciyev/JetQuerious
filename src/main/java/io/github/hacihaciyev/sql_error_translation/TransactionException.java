package io.github.hacihaciyev.sql_error_translation;

public class TransactionException extends RepositoryException {
    public TransactionException(String errorCode, String message) {
        super(errorCode, message);
    }
}
