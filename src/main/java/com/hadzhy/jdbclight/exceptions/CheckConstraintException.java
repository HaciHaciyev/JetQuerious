package com.hadzhy.jdbclight.exceptions;

public class CheckConstraintException extends RepositoryException {
    public CheckConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
