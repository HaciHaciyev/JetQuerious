package io.github.hacihaciyev.sql_error_translation;

public class MemoryAllocationException extends RepositoryException {
    public MemoryAllocationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
