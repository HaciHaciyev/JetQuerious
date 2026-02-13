package io.github.hacihaciyev.sql_error_translation;

public class ConnectionException extends RepositoryException {
    public ConnectionException(String errorCode, String message) {
        super(errorCode, message);
    }
}
