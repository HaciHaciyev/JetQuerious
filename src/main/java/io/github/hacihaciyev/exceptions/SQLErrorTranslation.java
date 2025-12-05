package io.github.hacihaciyev.exceptions;

import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Result;

import java.sql.SQLException;

public class SQLErrorTranslation {

    public static final String STATUS_CODE_FOR_COMMON_SITUATIONS = "HY000";

    private SQLErrorTranslation() {}

    /**
     * Handles SQL exceptions and translates them into application-specific exceptions
     * using standardized SQL state codes rather than vendor-specific error codes.
     *
     * @param e the {@code SQLException} to handle
     * @param <T> the type of the result
     * @return a {@code Result<T, Throwable>} containing the appropriate error
     */
    public static <T> Result<T, Exception> handleSQLException(final SQLException e) {
        final String sqlState = e.getSQLState();

        final String errorMessage = e.getMessage();
        if (sqlState == null)
            return new Err<>(new RepositoryException(STATUS_CODE_FOR_COMMON_SITUATIONS, "Database error occurred: " + errorMessage));

        final String substring;
        try {
            substring = sqlState.substring(0, 2);
        } catch (IndexOutOfBoundsException ie) {
            return new Err<>(new RepositoryException(sqlState, "Database error occurred: " + errorMessage));
        }

        return switch (substring) {
            case "01" -> new Err<>(new WarningException(sqlState, "Database warning: " + errorMessage));
            case "02" -> new Err<>(new NotFoundException(sqlState, "No data found or no rows affected: " + errorMessage));
            case "03" -> new Err<>(new IncompleteStatementException(sqlState, "SQL statement is not yet complete: " + errorMessage));
            case "07" -> new Err<>(new DynamicSQLException(sqlState, "Dynamic SQL error: " + errorMessage));
            case "0A" -> new Err<>(new UnsupportedFeatureException(sqlState, "Feature not supported: " + errorMessage));
            case "08" -> new Err<>(new ConnectionException(sqlState, "Database connection error: " + errorMessage));
            case "22" -> new Err<>(new DataException(sqlState, "Invalid data format or value out of range: " + errorMessage));
            case "23" -> handleIntegrityConstraintViolation(sqlState, errorMessage);
            case "24" -> new Err<>(new CursorException(sqlState, "Invalid cursor state: " + errorMessage));
            case "25" -> new Err<>(new TransactionException(sqlState, "Invalid transaction state: " + errorMessage));
            case "28" -> new Err<>(new AuthenticationException(sqlState, "Invalid authorization or authentication failed: " + errorMessage));
            case "40" -> handleTransactionRollbackException(sqlState, errorMessage);
            case "42" -> handleSyntaxOrAccessRuleViolation(sqlState, errorMessage);
            case "57" -> handleResourceNotAvailableException(sqlState, errorMessage);
            case "HY" -> handleCallLevelInterfaceException(sqlState, errorMessage);
            default -> new Err<>(new RepositoryException(sqlState, "Database error (SQL state " + sqlState + "): " + errorMessage));
        };
    }

    private static <T> Result<T, Exception> handleCallLevelInterfaceException(String sqlState, String errorMessage) {
        return new Err<>(switch (sqlState) {
            case "HY001" -> new MemoryAllocationException(sqlState, "Memory allocation error: " + errorMessage);
            case "HY008" -> new OperationCanceledException(sqlState, "Operation canceled: " + errorMessage);
            case "HY010" -> new FunctionSequenceException(sqlState, "Function sequence error: " + errorMessage);
            case "HY090" -> new InvalidArgumentException(sqlState, "Invalid string or buffer length: " + errorMessage);
            default -> new CLIException(sqlState, "CLI-specific error: " + errorMessage);
        });
    }

    private static <T> Result<T, Exception> handleResourceNotAvailableException(String sqlState, String errorMessage) {
        return new Err<>(switch (sqlState) {
            case "57033" -> new DeadlockException(sqlState, "Deadlock detected with another process: " + errorMessage);
            default -> new ResourceNotAvailableException(sqlState, "Resource not available or operator intervention: " + errorMessage);
        });
    }

    private static <T> Result<T, Exception> handleSyntaxOrAccessRuleViolation(String sqlState, String errorMessage) {
        return new Err<>(switch (sqlState) {
            case "42501" -> new InsufficientPrivilegeException(sqlState, "Insufficient privilege for the operation: " + errorMessage);
            case "42601" -> new SyntaxException(sqlState, "Syntax error in SQL statement: " + errorMessage);
            case "42703" -> new ColumnNotFoundException(sqlState, "Column not found in the table: " + errorMessage);
            case "42S01" -> new TableExistsException(sqlState, "Table already exists: " + errorMessage);
            case "42S02", "42P01" -> new TableNotFoundException(sqlState, "Table not found: " + errorMessage);
            case "42S21" -> new ColumnExistsException(sqlState, "Column already exists: " + errorMessage);
            case "42S22" -> new ColumnNotFoundException(sqlState, "Column not found: " + errorMessage);
            default -> new SyntaxOrAccessRuleException(sqlState, "Syntax error or access rule violation: " + errorMessage);
        });
    }

    private static <T> Result<T, Exception> handleTransactionRollbackException(String sqlState, String errorMessage) {
        return new Err<>(switch (sqlState) {
            case "40001" -> new DeadlockException(sqlState, "Deadlock detected. Transaction was rolled back: " + errorMessage);
            default -> new TransactionRollbackException(sqlState, "Transaction has been rolled back: " + errorMessage);
        });
    }

    private static <T> Result<T, Exception> handleIntegrityConstraintViolation(String sqlState, String errorMessage) {
        return new Err<>(switch (sqlState) {
            case "23001" -> new RestrictViolationException(sqlState, "Restrict delete rule has been violated: " + errorMessage);
            case "23502" -> new NullConstraintException(sqlState, "NOT NULL constraint violated: " + errorMessage);
            case "23503" -> new ForeignKeyConstraintException(sqlState, "Foreign key constraint violated:" + errorMessage);
            case "23505" -> new UniqueConstraintException(sqlState, "Unique constraint or index violated: " + errorMessage);
            case "23514" -> new CheckConstraintException(sqlState, "Check constraint violated: " + errorMessage);
            default -> new IntegrityConstraintException(sqlState, "An integrity constraint has been violated: " + errorMessage);
        });
    }
}