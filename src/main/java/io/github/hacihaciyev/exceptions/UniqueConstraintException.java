package io.github.hacihaciyev.exceptions;

public class UniqueConstraintException extends RepositoryException {
    public UniqueConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
