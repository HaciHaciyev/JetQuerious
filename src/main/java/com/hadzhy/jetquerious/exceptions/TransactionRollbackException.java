package com.hadzhy.jetquerious.exceptions;

public class TransactionRollbackException extends RepositoryException {
    public TransactionRollbackException(String errorCode, String message) {
        super(errorCode, message);
    }
}
