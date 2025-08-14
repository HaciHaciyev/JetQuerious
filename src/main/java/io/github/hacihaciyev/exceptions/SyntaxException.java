package io.github.hacihaciyev.exceptions;

public class SyntaxException extends RepositoryException {
    public SyntaxException(String errorCode, String message) {
        super(errorCode, message);
    }
}
