package com.hadzhy.jetquerious.exceptions;

public class ResourceNotAvailableException extends RepositoryException {
    public ResourceNotAvailableException(String errorCode, String message) {
        super(errorCode, message);
    }
}
