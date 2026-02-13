package io.github.hacihaciyev.sql_error_translation;

public class TransactionRollbackException extends RepositoryException {
    public TransactionRollbackException(String errorCode, String message) {
        super(errorCode, message);
    }
}
