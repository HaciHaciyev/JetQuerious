package com.hadzhy.jdbclight.exceptions;

public class DataException extends RepositoryException {
    public DataException(String errorCode, String message) {
        super(errorCode, message);
    }
}
