package com.hadzhy.jetquerious.exceptions;

public class UnsupportedFeatureException extends RepositoryException {
    public UnsupportedFeatureException(String errorCode, String message) {
        super(errorCode, message);
    }
}
