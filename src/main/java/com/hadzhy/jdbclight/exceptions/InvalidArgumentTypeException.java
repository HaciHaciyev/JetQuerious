package com.hadzhy.jdbclight.exceptions;

public class InvalidArgumentTypeException extends RepositoryException {

    private static final String DEFAULT_SQL_STATE = "22005";

    public InvalidArgumentTypeException(String message) {
        super(DEFAULT_SQL_STATE, message);
    }

    public InvalidArgumentTypeException(String errorCode, String message) {
        super(errorCode, message);
    }

    public InvalidArgumentTypeException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
