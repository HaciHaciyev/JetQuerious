package io.github.hacihaciyev.exceptions;

public class ConnectionException extends RepositoryException {
    public ConnectionException(String errorCode, String message) {
        super(errorCode, message);
    }
}
