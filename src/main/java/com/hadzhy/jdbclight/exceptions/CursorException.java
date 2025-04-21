package com.hadzhy.jdbclight.exceptions;

public class CursorException extends RepositoryException {
    public CursorException(String errorCode, String message) {
        super(errorCode, message);
    }
}
