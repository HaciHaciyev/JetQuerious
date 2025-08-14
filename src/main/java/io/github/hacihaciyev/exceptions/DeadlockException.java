package io.github.hacihaciyev.exceptions;

public class DeadlockException extends RepositoryException {
    public DeadlockException(String errorCode, String message) {
        super(errorCode, message);
    }
}
