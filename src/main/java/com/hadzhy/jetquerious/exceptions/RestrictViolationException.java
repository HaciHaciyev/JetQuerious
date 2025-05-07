package com.hadzhy.jetquerious.exceptions;

public class RestrictViolationException extends RepositoryException {
    public RestrictViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
