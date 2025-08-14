package io.github.hacihaciyev.exceptions;

public class SyntaxOrAccessRuleException extends RepositoryException {
    public SyntaxOrAccessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
