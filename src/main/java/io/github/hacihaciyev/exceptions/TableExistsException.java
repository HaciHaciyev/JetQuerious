package io.github.hacihaciyev.exceptions;

public class TableExistsException extends RepositoryException {
    public TableExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
