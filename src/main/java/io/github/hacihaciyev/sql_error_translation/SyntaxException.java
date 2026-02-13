package io.github.hacihaciyev.sql_error_translation;

public class SyntaxException extends RepositoryException {
    public SyntaxException(String errorCode, String message) {
        super(errorCode, message);
    }
}
