package com.hadzhy.jetquerious.exceptions;

public class MemoryAllocationException extends RepositoryException {
    public MemoryAllocationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
