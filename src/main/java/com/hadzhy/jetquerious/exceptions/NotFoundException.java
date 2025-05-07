package com.hadzhy.jetquerious.exceptions;

public class NotFoundException extends RepositoryException {

    private static final String DEFAULT_SQL_STATE = "02000";

    public NotFoundException(String message) {
        super(DEFAULT_SQL_STATE, message);
    }

    public NotFoundException(String sqlState, String message, Throwable cause) {
        super(sqlState, message, cause);
    }

    public NotFoundException(String sqlState, String message) {
        super(sqlState, message);
    }
}
