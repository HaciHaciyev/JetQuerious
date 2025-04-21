package com.hadzhy.jdbclight.exceptions;

public class ResourceNotAvailableException extends RepositoryException {
    public ResourceNotAvailableException(String errorCode, String message) {
        super(errorCode, message);
    }
}
