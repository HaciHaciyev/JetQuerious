package com.hadzhy.jdbclight.exceptions;

public class UnsupportedFeatureException extends RepositoryException {
    public UnsupportedFeatureException(String errorCode, String message) {
        super(errorCode, message);
    }
}
