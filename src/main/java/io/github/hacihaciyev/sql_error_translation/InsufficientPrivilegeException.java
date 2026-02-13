package io.github.hacihaciyev.sql_error_translation;

public class InsufficientPrivilegeException extends RepositoryException {
    public InsufficientPrivilegeException(String errorCode, String message) {
        super(errorCode, message);
    }
}
