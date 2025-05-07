package com.hadzhy.jetquerious.exceptions;

public class CheckConstraintException extends RepositoryException {
    public CheckConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
