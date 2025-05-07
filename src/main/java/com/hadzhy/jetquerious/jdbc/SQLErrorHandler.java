package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.*;
import com.hadzhy.jetquerious.util.Result;

import java.sql.SQLException;

public class SQLErrorHandler {

    public static final String STATUS_CODE_FOR_COMMON_SITUATIONS = "HY000";

    /**
     * Handles SQL exceptions and translates them into application-specific exceptions
     * using standardized SQL state codes rather than vendor-specific error codes.
     *
     * @param e the {@code SQLException} to handle
     * @param <T> the type of the result
     * @return a {@code Result<T, Throwable>} containing the appropriate error
     */
    public static <T> Result<T, Throwable> handleSQLException(final SQLException e) {
        final String sqlState = e.getSQLState();

        // If SQL state is null, fallback to default handling
        final String errorMessage = e.getMessage();
        if (sqlState == null)
            return Result.failure(new RepositoryException(STATUS_CODE_FOR_COMMON_SITUATIONS, "Database error occurred: " + errorMessage));

        // Handle based on standardized SQL state categories
        final String substring;
        try {
            substring = sqlState.substring(0, 2);
        } catch (IndexOutOfBoundsException ie) {
            return Result.failure(new RepositoryException(sqlState, "Database error occurred: " + errorMessage));
        }

        return switch (substring) {
            // Class 01: Warning
            case "01" -> Result.failure(new WarningException(sqlState, "Database warning: " + errorMessage));

            // Class 02: No Data
            case "02" -> Result.failure(new NotFoundException(sqlState, "No data found or no rows affected: " + errorMessage));

            // Class 03: SQL Statement Not Yet Complete
            case "03" -> Result.failure(new IncompleteStatementException(sqlState, "SQL statement is not yet complete: " + errorMessage));

            // Dynamic sql exceptions
            case "07" -> Result.failure(new DynamicSQLException(sqlState, "Dynamic SQL error: " + errorMessage));

            // Feature not supported exception
            case "0A" -> Result.failure(new UnsupportedFeatureException(sqlState, "Feature not supported: " + errorMessage));

            // Class 08: Connection Exception
            case "08" -> Result.failure(new ConnectionException(sqlState, "Database connection error: " + errorMessage));

            // Class 22: Data Exception
            case "22" -> Result.failure(new DataException(sqlState, "Invalid data format or value out of range: " + errorMessage));

            // Class 23: Integrity Constraint Violation
            case "23" -> handleIntegrityConstraintViolation(sqlState, errorMessage);

            // Class 24: Invalid Cursor State
            case "24" -> Result.failure(new CursorException(sqlState, "Invalid cursor state: " + errorMessage));

            // Class 25: Invalid Transaction State
            case "25" -> Result.failure(new TransactionException(sqlState, "Invalid transaction state: " + errorMessage));

            // Class 28: Invalid Authorization Specification
            case "28" -> Result.failure(new AuthenticationException(sqlState, "Invalid authorization or authentication failed: " + errorMessage));

            // Class 40: Transaction Rollback
            case "40" -> handleTransactionRollbackException(sqlState, errorMessage);

            // Class 42: Syntax Error or Access Rule Violation
            case "42" -> handleSyntaxOrAccessRuleViolation(sqlState, errorMessage);

            // Class 57: Resource Not Available or Operator Intervention
            case "57" -> handleResourceNotAvailableException(sqlState, errorMessage);

            // Class HT: Call level interface errors
            case "HY" -> handleCallLevelInterfaceException(sqlState, errorMessage);

            // Overall
            default -> Result.failure(new RepositoryException(sqlState, "Database error (SQL state " + sqlState + "): " + errorMessage));
        };
    }

    private static <T> Result<T, Throwable> handleCallLevelInterfaceException(String sqlState, String errorMessage) {
        return switch (sqlState) {
            case "HY001" -> Result.failure(new MemoryAllocationException(sqlState, "Memory allocation error: " + errorMessage));
            case "HY008" -> Result.failure(new OperationCanceledException(sqlState, "Operation canceled: " + errorMessage));
            case "HY010" -> Result.failure(new FunctionSequenceException(sqlState, "Function sequence error: " + errorMessage));
            case "HY090" -> Result.failure(new InvalidArgumentException(sqlState, "Invalid string or buffer length: " + errorMessage));
            default -> Result.failure(new CLIException(sqlState, "CLI-specific error: " + errorMessage));
        };
    }

    private static <T> Result<T, Throwable> handleResourceNotAvailableException(String sqlState, String errorMessage) {
        return switch (sqlState) {
            case "57033" -> Result.failure(new DeadlockException(sqlState, "Deadlock detected with another process: " + errorMessage));
            case "57P01" ->
                    Result.failure(new ShutdownException(sqlState, "Database server shutdown while operation in progress: " + errorMessage));
            default ->
                    Result.failure(new ResourceNotAvailableException(sqlState, "Resource not available or operator intervention: " + errorMessage));
        };
    }

    private static <T> Result<T, Throwable> handleSyntaxOrAccessRuleViolation(String sqlState, String errorMessage) {
        return switch (sqlState) {
            case "42501" -> Result.failure(new InsufficientPrivilegeException(sqlState, "Insufficient privilege for the operation: " + errorMessage));
            case "42601" -> Result.failure(new SyntaxException(sqlState, "Syntax error in SQL statement: " + errorMessage));
            case "42703" -> Result.failure(new ColumnNotFoundException(sqlState, "Column not found in the table: " + errorMessage));
            case "42P01" -> Result.failure(new TableNotFoundException(sqlState, "Table does not exist: " + errorMessage));
            case "42S01" -> Result.failure(new TableExistsException(sqlState, "Table already exists: " + errorMessage));
            case "42S02" -> Result.failure(new TableNotFoundException(sqlState, "Table not found: " + errorMessage));
            case "42S21" -> Result.failure(new ColumnExistsException(sqlState, "Column already exists: " + errorMessage));
            case "42S22" -> Result.failure(new ColumnNotFoundException(sqlState, "Column not found: " + errorMessage));
            default -> Result.failure(new SyntaxOrAccessRuleException(sqlState, "Syntax error or access rule violation: " + errorMessage));
        };
    }

    private static <T> Result<T, Throwable> handleTransactionRollbackException(String sqlState, String errorMessage) {
        return switch (sqlState) {
            case "40001" -> Result.failure(new DeadlockException(sqlState, "Deadlock detected. Transaction was rolled back: " + errorMessage));
            case "40P01" -> Result.failure(new DeadlockException(sqlState, "Deadlock detected in PostgreSQL. Transaction was rolled back: " + errorMessage));
            default -> Result.failure(new TransactionRollbackException(sqlState, "Transaction has been rolled back: " + errorMessage));
        };
    }

    private static <T> Result<T, Throwable> handleIntegrityConstraintViolation(String sqlState, String errorMessage) {
        return switch (sqlState) {
            case "23001" -> Result.failure(new RestrictViolationException(sqlState, "Restrict delete rule has been violated: " + errorMessage));
            case "23502" -> Result.failure(new NullConstraintException(sqlState, "NOT NULL constraint violated: " + errorMessage));
            case "23503" -> Result.failure(new ForeignKeyConstraintException(sqlState, "Foreign key constraint violated:" + errorMessage));
            case "23505" -> Result.failure(new UniqueConstraintException(sqlState, "Unique constraint or index violated: " + errorMessage));
            case "23514" -> Result.failure(new CheckConstraintException(sqlState, "Check constraint violated: " + errorMessage));
            default -> Result.failure(new IntegrityConstraintException(sqlState, "An integrity constraint has been violated: " + errorMessage));
        };
    }
}
