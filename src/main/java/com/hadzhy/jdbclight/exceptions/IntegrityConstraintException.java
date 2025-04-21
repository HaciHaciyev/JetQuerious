package com.hadzhy.jdbclight.exceptions;

public class IntegrityConstraintException extends RepositoryException {
    public IntegrityConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
