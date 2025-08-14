package io.github.hacihaciyev.exceptions;

public class ForeignKeyConstraintException extends RepositoryException {
    public ForeignKeyConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
