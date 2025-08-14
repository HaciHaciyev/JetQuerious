package io.github.hacihaciyev.exceptions;

public class ColumnExistsException extends RepositoryException {
    public ColumnExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
