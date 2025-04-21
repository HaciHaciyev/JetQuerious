package com.hadzhy.jdbclight.exceptions;

import java.util.*;

/**
 * Base exception for all repository-related errors.
 * Features:
 * - Machine-readable `errorCode` (e.g., "REPO_NOT_FOUND")
 * - Human-readable `message` (required)
 * - Extensible `context` for metadata (e.g., SQL state, query)
 * - Full exception chaining (cause)
 */
public class RepositoryException extends RuntimeException {
    private final String sqlErrorCode;
    private final Map<String, Object> context;

    public RepositoryException(String sqlErrorCode, String message) {
        super(Objects.requireNonNull(message, "Message cannot be null"));
        this.sqlErrorCode = Objects.requireNonNull(sqlErrorCode, "Error code cannot be null");
        this.context = new HashMap<>();
    }

    public RepositoryException(String sqlErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.sqlErrorCode = sqlErrorCode;
        this.context = new HashMap<>();
    }

    public Optional<String> sqlErrorCode() {
        return Optional.ofNullable(sqlErrorCode);
    }

    public Map<String, Object> context() {
        return Collections.unmodifiableMap(context);
    }

    public RepositoryException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s [code=%s, context=%s]", super.toString(), sqlErrorCode, context);
    }
}