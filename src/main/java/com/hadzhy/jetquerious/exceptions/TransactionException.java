package com.hadzhy.jetquerious.exceptions;

public class TransactionException extends RepositoryException {
    public TransactionException(String errorCode, String message) {
        super(errorCode, message);
    }
}
