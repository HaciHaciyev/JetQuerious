package io.github.hacihaciyev.exceptions;

public class TableNotFoundException extends RepositoryException {
    public TableNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
