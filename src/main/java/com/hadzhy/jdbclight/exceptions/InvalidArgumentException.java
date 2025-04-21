package com.hadzhy.jdbclight.exceptions;

public class InvalidArgumentException extends RepositoryException {
    public InvalidArgumentException(String errorCode, String message) {
        super(errorCode, message);
    }
}
