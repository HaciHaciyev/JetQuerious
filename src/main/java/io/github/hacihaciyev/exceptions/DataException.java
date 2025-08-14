package io.github.hacihaciyev.exceptions;

public class DataException extends RepositoryException {
    public DataException(String errorCode, String message) {
        super(errorCode, message);
    }
}
