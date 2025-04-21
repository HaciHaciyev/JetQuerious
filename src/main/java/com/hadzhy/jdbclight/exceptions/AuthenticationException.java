package com.hadzhy.jdbclight.exceptions;

public class AuthenticationException extends RepositoryException {
    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
