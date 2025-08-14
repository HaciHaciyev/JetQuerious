package io.github.hacihaciyev.exceptions;

public class IntegrityConstraintException extends RepositoryException {
    public IntegrityConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
