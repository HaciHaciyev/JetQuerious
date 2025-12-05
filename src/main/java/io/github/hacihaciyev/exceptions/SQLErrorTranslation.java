package io.github.hacihaciyev.exceptions;

import io.github.hacihaciyev.util.Err;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;

public class SQLErrorTranslation {

    public static final String STATUS_CODE_FOR_COMMON_SITUATIONS = "HY000";

    private SQLErrorTranslation() {}

    public enum SQLStateRegistry {
        WARNING("01", WarningException::new),
        NOT_FOUND("02", NotFoundException::new),
        INCOMPLETE_STATEMENT("03", IncompleteStatementException::new),
        DYNAMIC_SQL("07", DynamicSQLException::new),
        UNSUPPORTED_FEATURE("0A", UnsupportedFeatureException::new),
        CONNECTION("08", ConnectionException::new),
        DATA_ERROR("22", DataException::new),
        CURSOR_ERROR("24", CursorException::new),
        TRANSACTION_ERROR("25", TransactionException::new),
        AUTHENTICATION("28", AuthenticationException::new),

        RESTRICT_VIOLATION("23001", (code, msg) ->
                new RestrictViolationException(code, "Restrict delete rule violated: " + msg)),

        NULL_CONSTRAINT("23502", (code, msg) ->
                new NullConstraintException(code, "NOT NULL constraint violated: " + msg)),

        FOREIGN_KEY_CONSTRAINT("23503", (code, msg) ->
                new ForeignKeyConstraintException(code, "Foreign key constraint violated: " + msg)),

        UNIQUE_CONSTRAINT("23505", (code, msg) ->
                new UniqueConstraintException(code, "Unique constraint violated: " + msg)),

        CHECK_CONSTRAINT("23514", (code, msg) ->
                new CheckConstraintException(code, "Check constraint violated: " + msg)),

        DEADLOCK("40001", (code, msg) ->
                new DeadlockException(code, "Deadlock detected. Transaction rolled back: " + msg)),

        HY001("HY001", MemoryAllocationException::new),
        HY008("HY008", OperationCanceledException::new),
        HY010("HY010", FunctionSequenceException::new),
        HY090("HY090", InvalidArgumentException::new),

        RESOURCE_NOT_AVAILABLE("57", ResourceNotAvailableException::new),
        DEADLOCK_RESOURCE("57033", (code, msg) ->
                new DeadlockException(code, "Deadlock detected with another process: " + msg));

        private final String code;
        private final BiFunction<String, String, Exception> constructor;

        SQLStateRegistry(String code, BiFunction<String, String, Exception> constructor) {
            this.code = code;
            this.constructor = constructor;
        }

        public static Optional<BiFunction<String, String, Exception>> find(String sqlState) {
            return Arrays.stream(values())
                    .filter(v -> sqlState.equals(v.code) || sqlState.startsWith(v.code))
                    .map(v -> v.constructor)
                    .findFirst();
        }
    }

    public static <T> Err<T, Exception> handleSQLException(final SQLException e) {
        String sqlState = e.getSQLState();
        String message = e.getMessage();

        if (sqlState == null || sqlState.isBlank())
            return new Err<>(new RepositoryException(
                    STATUS_CODE_FOR_COMMON_SITUATIONS, "Database error occurred: " + message));

        return SQLStateRegistry.find(sqlState)
                .map(f -> new Err<T, Exception>(f.apply(sqlState, message)))
                .orElseGet(() -> new Err<>(new RepositoryException(sqlState, "Database error (SQL state " + sqlState + "): " + message)));
    }
}