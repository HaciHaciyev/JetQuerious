package com.hadzhy.jdbclight.exceptions;

public class FunctionSequenceException extends RepositoryException {
    public FunctionSequenceException(String errorCode, String message) {
        super(errorCode, message);
    }
}
