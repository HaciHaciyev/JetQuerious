package com.hadzhy.jetquerious.exceptions;

public class DynamicSQLException extends RepositoryException {
    public DynamicSQLException(String errorCode, String message) {
        super(errorCode, message);
    }
}
