package io.github.hacihaciyev.sql_error_translation;

public class ResourceNotAvailableException extends RepositoryException {
    public ResourceNotAvailableException(String errorCode, String message) {
        super(errorCode, message);
    }
}
