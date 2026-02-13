package io.github.hacihaciyev.sql_error_translation;

public class SyntaxOrAccessRuleException extends RepositoryException {
    public SyntaxOrAccessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
