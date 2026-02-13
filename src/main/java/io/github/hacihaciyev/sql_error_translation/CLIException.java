package io.github.hacihaciyev.sql_error_translation;

public class CLIException extends RepositoryException {
    public CLIException(String errorCode, String message) {
        super(errorCode, message);
    }
}
