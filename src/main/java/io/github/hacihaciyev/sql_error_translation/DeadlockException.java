package io.github.hacihaciyev.sql_error_translation;

public class DeadlockException extends RepositoryException {
    public DeadlockException(String errorCode, String message) {
        super(errorCode, message);
    }
}
