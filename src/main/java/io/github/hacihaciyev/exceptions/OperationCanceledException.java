package io.github.hacihaciyev.exceptions;

public class OperationCanceledException extends RepositoryException {
    public OperationCanceledException(String errorCode, String message) {
        super(errorCode, message);
    }
}
