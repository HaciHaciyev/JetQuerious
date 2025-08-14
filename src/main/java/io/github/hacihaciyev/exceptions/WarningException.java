package io.github.hacihaciyev.exceptions;

public class WarningException extends RepositoryException {
    public WarningException(String errorCode, String message) {
        super(errorCode, message);
    }
}
