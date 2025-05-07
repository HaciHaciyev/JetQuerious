package com.hadzhy.jetquerious.exceptions;

public class IncompleteStatementException extends RepositoryException {
    public IncompleteStatementException(String errorCode, String message) {
        super(errorCode, message);
    }
}
