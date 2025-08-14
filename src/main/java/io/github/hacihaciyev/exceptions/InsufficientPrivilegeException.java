package io.github.hacihaciyev.exceptions;

public class InsufficientPrivilegeException extends RepositoryException {
    public InsufficientPrivilegeException(String errorCode, String message) {
        super(errorCode, message);
    }
}
