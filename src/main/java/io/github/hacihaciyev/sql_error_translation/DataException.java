package io.github.hacihaciyev.sql_error_translation;

public class DataException extends RepositoryException {
    public DataException(String errorCode, String message) {
        super(errorCode, message);
    }
}
