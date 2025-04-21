package com.hadzhy.jdbclight.exceptions;

public class ShutdownException extends RepositoryException {
    public ShutdownException(String errorCode, String message) {
        super(errorCode, message);
    }
}
