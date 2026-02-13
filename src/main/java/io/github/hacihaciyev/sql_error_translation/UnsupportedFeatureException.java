package io.github.hacihaciyev.sql_error_translation;

public class UnsupportedFeatureException extends RepositoryException {
    public UnsupportedFeatureException(String errorCode, String message) {
        super(errorCode, message);
    }
}
