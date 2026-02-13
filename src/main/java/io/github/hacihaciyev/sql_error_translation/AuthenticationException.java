package io.github.hacihaciyev.sql_error_translation;

public class AuthenticationException extends RepositoryException {
    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
