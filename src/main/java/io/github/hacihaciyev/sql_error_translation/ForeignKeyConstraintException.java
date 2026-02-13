package io.github.hacihaciyev.sql_error_translation;

public class ForeignKeyConstraintException extends RepositoryException {
    public ForeignKeyConstraintException(String errorCode, String message) {
        super(errorCode, message);
    }
}
