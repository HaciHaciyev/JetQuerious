package io.github.hacihaciyev.sql_error_translation;

public class FunctionSequenceException extends RepositoryException {
    public FunctionSequenceException(String errorCode, String message) {
        super(errorCode, message);
    }
}
