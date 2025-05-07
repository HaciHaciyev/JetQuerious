package com.hadzhy.jetquerious.exceptions;

public class NullConstraintException extends RepositoryException {
    public NullConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
