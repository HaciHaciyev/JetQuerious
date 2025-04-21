package com.hadzhy.jdbclight.exceptions;

public class ColumnNotFoundException extends RepositoryException {
    public ColumnNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
