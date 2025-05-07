package com.hadzhy.jetquerious.exceptions;

public class CLIException extends RepositoryException {
    public CLIException(String errorCode, String message) {
        super(errorCode, message);
    }
}
