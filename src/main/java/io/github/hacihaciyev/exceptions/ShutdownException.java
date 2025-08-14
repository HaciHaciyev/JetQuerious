package io.github.hacihaciyev.exceptions;

public class ShutdownException extends RepositoryException {
    public ShutdownException(String errorCode, String message) {
        super(errorCode, message);
    }
}
