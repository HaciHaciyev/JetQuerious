package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.NotFoundException;
import com.hadzhy.jetquerious.exceptions.InvalidArgumentTypeException;
import com.hadzhy.jetquerious.exceptions.TransactionException;
import com.hadzhy.jetquerious.util.Nullable;
import com.hadzhy.jetquerious.util.Result;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hadzhy.jetquerious.jdbc.SQLErrorHandler.handleSQLException;

/**
 * The {@code JetQuerious} class provides a set of utility methods for interacting with a relational database using JDBC (Java Database Connectivity).
 * It simplifies the execution of SQL queries and the handling of results, while also providing error handling and transaction management.
 * This class is designed to be used as a singleton within a DI container.
 *
 * <h2>Class API Overview</h2>
 * The {@code JetQuerious} class offers the following key functionalities:
 * <ul>
 *     <li><strong>Read Operations:</strong> Methods to execute SQL queries that return single values, objects, or lists of objects.</li>
 *     <li><strong>Write Operations:</strong> Methods to execute SQL updates, including single updates, updates with array parameters, and batch updates.</li>
 *     <li><strong>Error Handling:</strong> Built-in mechanisms to handle SQL exceptions and translate them into application-specific exceptions.</li>
 *     <li><strong>Transaction Management:</strong> Automatic management of transactions for write operations, ensuring data integrity.</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 * To use the {@code JetQuerious} class effectively, follow these guidelines:
 * <ol>
 *     <li><strong>Initialization:</strong> Ensure that the {@code JetQuerious} instance is properly initialized with a valid {@code DataSource} before use.</li>
 *     <li><strong>Read Operations:</strong> Use the {@code read()} method to fetch single values or objects. For complex mappings, consider using the {@code read()} method with a {@code ResultSetExtractor} or {@code RowMapper}.</li>
 *     <li><strong>Write Operations:</strong> Use the {@code write()} method for standard updates. For updates involving arrays, use {@code writeArrayOf()}. For bulk operations, utilize {@code writeBatch()}.</li>
 *     <li><strong>Parameter Handling:</strong> Always ensure that parameters passed to SQL statements are properly sanitized and validated to prevent SQL injection attacks.</li>
 *     <li><strong>Error Handling:</strong> Check the result of each operation. Use the {@code Result} object to determine success or failure and handle errors appropriately.</li>
 *     <li><strong>Connection Management:</strong> The class manages connections internally, so there is no need to manually open or close connections. However, ensure that the {@code DataSource} is properly configured.</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Initialize the JetQuerious instance with a DataSource
 * DataSource dataSource = ...; // Obtain a DataSource instance
 * JetQuerious.init(dataSource);
 * JetQuerious jetQuerious = JetQuerious.instance();
 *
 * // Insert a new product. Use static import for SQLBuilder.insert()
 * String insertSQL = insert()
 *      .into("products")
 *      .column("name")
 *      .column("price")
 *      .values()
 *      .build()
 *      .sql();
 *
 * Result<Boolean, Throwable> insertResult = jetQuerious.write(insertSQL, "New Product", 29.99);
 *
 * // Fetch a product by ID
 * String selectSQL = select()
 *      .all()
 *      .from("products")
 *      .where("id = ?")
 *      .build()
 *      .sql();
 *
 * Result<Product, Throwable> productResult = jetQuerious.read(selectSQL, Product.class, 1);
 *
 * // Update product tags
 * String updateTagsSQL = update("products")
 *      .set("tags = ?")
 *      .where("id = ?")
 *      .build()
 *      .sql();
 *
 * String[] tags = {"electronics", "sale"};
 * Result<Boolean, Throwable> updateResult = jetQuerious.writeArrayOf(updateTagsSQL, "text", 1, tags, 1);
 *
 * // Batch insert customers
 * String batchInsertSQL = insert()
 *             .into("customers")
 *             .columns("name", "email")
 *             .values()
 *             .build()
 *             .sql();
 *
 * List<Object[]> batchArgs = Arrays.asList(
 *     new Object[]{"Alice", "alice@example.com"},
 *     new Object[]{"Bob", "bob@example.com"}
 * );
 *
 * Result<Boolean, Throwable> batchResult = jetQuerious.writeBatch(batchInsertSQL, batchArgs);
 * }</pre>
 *
 * @author Hadzhyiev Hadzhy
 * @version 3.0
 */
public class JetQuerious {
    private final DataSource dataSource;
    private static volatile JetQuerious instance;
    private static final Map<Class<?>, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Logger LOG = Logger.getLogger(JetQuerious.class.getName());
    private static final Map<String, Class<?>> SUPPORTED_ARRAY_TYPES = Map.ofEntries(
            Map.entry("text", String.class),
            Map.entry("varchar", String.class),
            Map.entry("int", Integer.class),
            Map.entry("integer", Integer.class),
            Map.entry("bigint", Long.class),
            Map.entry("boolean", Boolean.class),
            Map.entry("uuid", java.util.UUID.class),
            Map.entry("date", java.sql.Date.class),
            Map.entry("timestamp", java.sql.Timestamp.class)
    );


    private JetQuerious(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static void init(DataSource dataSource) {
        if (instance == null) synchronized (JetQuerious.class) {
            if (instance == null) instance = new JetQuerious(dataSource);
        }
    }

    public static JetQuerious instance() {
        if (instance == null)
            throw new IllegalStateException("JetQuerious is not initialized. Call JDBC.init(dataSource) first.");
        return instance;
    }

    /**
     * Executes the given action within a single database transaction, ensuring atomicity.
     * Automatically manages the connection lifecycle: it opens a connection, disables auto-commit,
     * executes the action, and then either commits the changes or rolls them back in case of an error.
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Result<Void, Throwable> result = jetQuerious.transactional(conn -> {
     *     jetQuerious.stepInTransaction(conn, "INSERT INTO users (name) VALUES (?)", "Bob");
     *     jetQuerious.stepInTransaction(conn, "INSERT INTO logs (event) VALUES (?)", "User created");
     * });
     * }</pre>
     *
     * @param action the database operations to perform within the transaction,
     *               receiving a {@link Connection} object
     * @return {@code Result.success(null)} if the transaction was committed successfully;
     *         otherwise, {@code Result.failure(throwable)} with the exception that caused rollback
     *
     * <p><b>Note:</b></p>
     * <ul>
     *   <li>This method automatically closes the connection when the transaction completes.</li>
     *   <li>If the action throws an exception, the transaction is rolled back.</li>
     *   <li>All operations within the transaction must use the provided {@code Connection} object.</li>
     *   <li>Use {@link #stepInTransaction} for executing SQL within the transaction context.</li>
     * </ul>
     *
     * <p><b>⚠ Important:</b></p>
     * <p>
     * Only operations performed via {@code stepInTransaction} will be part of the transaction.
     * Avoid executing raw JDBC calls or using other data access methods within the transactional lambda,
     * unless you are certain they respect the same connection and transaction context.
     * </p>
     * <p>
     * Operations that are <b>not</b> part of the transaction include:
     * </p>
     * <ul>
     *   <li>Direct JDBC calls using the connection object</li>
     *   <li>Calls to methods that do not use {@code stepInTransaction}</li>
     *   <li>Custom SQL execution utilities not designed for transactional use</li>
     * </ul>
     * <p>
     * These operations will execute independently and <b>will not</b> be rolled back
     * if the transaction fails.
     * </p>
     */
    public Result<Void, Throwable> transactional(SQLConsumer<Connection> action) {
        if (action == null) return Result.failure(new IllegalArgumentException("Action must not be null"));

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                action.accept(connection);
                connection.commit();
                return Result.success(null);
            } catch (Throwable t) {
                connection.rollback();
                return Result.failure(t);
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Exception during transaction", t);
            return Result.failure(t);
        }
    }

    /**
     * Executes a single SQL update operation using a provided connection and parameters.
     * <p>
     * This method is intended for internal use within transactional blocks managed by
     * {@link #transactional(SQLConsumer)}. It prepares and executes the given SQL statement
     * with the provided parameters using the given JDBC connection.
     * <p>
     * The method does not handle connection management or transactional boundaries itself.
     * It relies on the caller to ensure the connection is part of a valid transaction and
     * properly managed.
     * <p>
     * Usage is expected only via the lambda passed to {@code transactional()}, for example:
     *
     * <pre>{@code
     * jetQuerious.transactional(conn -> {
     *     jetQuerious.stepInTransaction(conn, "UPDATE accounts SET balance = balance - ? WHERE id = ?", 100, 1);
     *     jetQuerious.stepInTransaction(conn, "UPDATE accounts SET balance = balance + ? WHERE id = ?", 100, 2);
     * });
     * }</pre>
     *
     * @param connection the JDBC connection to use, must be managed externally (e.g., from {@code transactional()})
     * @param sql        the SQL update statement to execute (must not be null)
     * @param params     optional parameters to bind to the prepared statement
     *
     * @throws IllegalArgumentException if the connection or sql is null
     * @throws TransactionException if a database access error occurs during execution
     *
     * <p><b>Note:</b></p>
     * - This method does not perform any transaction commit or rollback.
     * - It should never be called outside the context of {@code transactional()}.
     * - Exceptions are thrown directly to propagate transactional failure.
     */
    public void stepInTransaction(final Connection connection, final String sql,
                                  final @Nullable Object... params) {

        if (connection == null) throw new IllegalArgumentException("Connection cannot be null");
        if (sql == null) throw new IllegalArgumentException("SQL cannot be null");
        validateArgumentsTypes(params);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, params);
            statement.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Exception during step in transaction", e);
            throw new TransactionException(e.getSQLState(), e.getMessage());
        }
    }

    /**
     * Executes a custom SQL operation using the provided SQL query and a callback function
     * to operate on a {@link PreparedStatement}. The callback allows the user to control
     * the execution logic (e.g., executeQuery, executeUpdate) and handle the result.
     *
     * <p>The method ensures that the connection and PreparedStatement are closed after execution,
     * and it provides a safe way to handle SQL exceptions.</p>
     *
     * @param <T> The type of result returned by the callback function.
     * @param sql The SQL query string to be executed.
     * @param callback The callback function that will operate on the {@link PreparedStatement}.
     *                 It can execute queries, updates, or batch operations, and return a result.
     * @param params The parameters to be set in the PreparedStatement.
     *               They are set in the order they appear in the SQL query.
     *
     * @return A {@link Result} object containing the result of the callback execution or an error if the operation fails.
     *         If the operation succeeds, the result is encapsulated in {@code Result.success(T)}.
     *         If an error occurs, it is encapsulated in {@code Result.failure(Throwable)}.
     *
     * @see PreparedStatement
     * @see SQLFunction
     *
     * <p><b>Example usage:</b></p>
     *
     * <pre>{@code
     * String sql = "SELECT name, age FROM users WHERE id = ?";
     * int userId = 1;
     *
     * Result&lt;List&lt;String&gt;, Throwable&gt; result = jetQuerious.execute(
     *     sql,
     *     statement -> {
     *         ResultSet resultSet = statement.executeQuery();
     *         List&lt;String&gt; userInfo = new ArrayList&lt;&gt;();
     *         while (resultSet.next()) {
     *             userInfo.add(resultSet.getString("name"));
     *             userInfo.add(String.valueOf(resultSet.getInt("age")));
     *         }
     *         return userInfo;
     *     },
     *     userId
     * );
     *
     * if (result.isSuccess()) {
     *     List&lt;String&gt; user = result.get();
     *     System.out.println("User info: " + user);
     * } else {
     *     System.err.println("Error: " + result.getError().getMessage());
     * }
     * }</pre>
     */
    public <T> Result<T, Throwable> execute(final String sql, final SQLFunction<PreparedStatement, T> callback,
                                            final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (callback == null) return Result.failure(new IllegalArgumentException("Callback function cannot be null"));
        validateArgumentsTypes(params);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, params);

            T result = callback.apply(statement);
            return Result.success(result);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Exception during custom execute", e);
            return handleSQLException(e);
        }
    }

    public <T> CompletableFuture<Result<T, Throwable>> asynchExecute(final String sql, final SQLFunction<PreparedStatement, T> callback,
                                                                     final @Nullable Object... params) {

        return CompletableFuture.supplyAsync(() -> execute(sql, callback, params));
    }

    /**
     * Executes a SQL query and uses a {@code ResultSetExtractor} to map the result set to an object.
     *
     * @param sql      the SQL query to execute
     * @param extractor a functional interface for extracting data from the {@code ResultSet}
     * @param params   optional parameters for the SQL query
     * @param <T>     the type of the extracted object
     * @return a {@code Result<T, Throwable>} containing the extracted object or an error
     * @throws NullPointerException if {@code sql} or {@code extractor} is {@code null}
     *
     * <pre>{@code
     * Result<UserAccount, Throwable> userResult = jetQuerious.read(
     *          FIND_BY_ID,
     *          this::userAccountMapper,
     *          userId.toString()
     * );
     * }</pre>
     */
    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor, final @Nullable Object... params) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        validateArgumentsTypes(params);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new NotFoundException("Data in for this query was not found."));
                }

                T value = extractor.extractData(resultSet);
                return Result.success(value);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor,
                                         final ResultSetType resultSetType, final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        if (resultSetType == null) return Result.failure(new IllegalArgumentException("Result set type can`t be null"));
        validateArgumentsTypes(params);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, resultSetType.type(), resultSetType.concurrency())) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new NotFoundException("Data in for this query was not found."));
                }

                T value = extractor.extractData(resultSet);
                return Result.success(value);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public <T> CompletableFuture<Result<T, Throwable>> asynchRead(final String sql, final ResultSetExtractor<T> extractor,
                                                                  final @Nullable Object... params) {

        return CompletableFuture.supplyAsync(() -> read(sql, extractor, params));
    }

    public <T> CompletableFuture<Result<T, Throwable>> asynchRead(final String sql, final ResultSetExtractor<T> extractor,
                                                                  final ResultSetType resultSetType, final @Nullable Object... params) {
        return CompletableFuture.supplyAsync(() -> read(sql, extractor, resultSetType, params));
    }

    /**
     * Executes a SQL query that returns a single value of a specified wrapper type.
     *
     * @param sql  the SQL query to execute
     * @param type the expected return type, which must be a wrapper type (e.g., Integer.class)
     * @param params optional parameters for the SQL query
     * @param <T>  the type of the result
     * @return a {@code Result<T, Throwable>} containing the result or an error
     * @throws NullPointerException if {@code sql} or {@code type} is {@code null}
     * @throws InvalidArgumentTypeException if the specified type is not a valid wrapper type
     *
     * <pre>{@code
     * Integer count = jetQuerious.readObjectOf(
     *          "SELECT COUNT(email) FROM YOU_TABLE WHERE email = ?",
     *          Integer.class,
     *          verifiableEmail.email()
     * )
     *          .orElseThrow();
     * }</pre>
     */
    public <T> Result<T, Throwable> readObjectOf(final String sql, final Class<T> type, final @Nullable Object... params) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (type == null) return Result.failure(new IllegalArgumentException("Type cannot be null"));
        for (Object param : params) {
            if (param instanceof Enum<?>) {
                throw new InvalidArgumentTypeException("\"Enum conversion is not supported directly. Use specific enum type or handle separately.");
            }
        }
        validateArgumentsTypes(params);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new NotFoundException("Data in query for object was not found."));
                }

                T value = Mapper.map(resultSet, type);
                return Result.success(value);
            }
        } catch (SQLException | IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            if (e instanceof IllegalArgumentException) {
                return Result.failure(e);
            }

            return handleSQLException((SQLException) e);
        }
    }

    public <T> CompletableFuture<Result<T, Throwable>> asynchReadObjectOf(final String sql, final Class<T> type,
                                                                          final @Nullable Object... params) {

       return CompletableFuture.supplyAsync(() -> readObjectOf(sql, type, params));
    }

    /**
     * Executes a SQL query that returns a list of objects mapped by a {@code RowMapper}.
     *
     * @param sql      the SQL query to execute
     * @param extractor a functional interface for mapping rows of the result set to objects
     * @param <T>     the type of the mapped objects
     * @return a {@code Result<List<T>, Throwable>} containing the list of mapped objects or an error
     * @throws NullPointerException if {@code sql} or {@code rowMapper} is {@code null}
     *
     * <pre>{@code
     * Result<List<UserAccount>, Throwable> users = jetQuerious.readListOf(
     *          "SELECT * FROM UserAccount",
     *          this::userAccountMapper
     * );
     * }</pre>
     */
    public <T> Result<List<T>, Throwable> readListOf(final String sql, final ResultSetExtractor<T> extractor,
                                                     final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        validateArgumentsTypes(params);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql,
                     ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            final List<T> results = new ArrayList<>();
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    T item = extractor.extractData(resultSet);
                    results.add(item);
                }
            }

            return Result.success(results);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public <T> Result<List<T>, Throwable> readListOf(final String sql, final ResultSetExtractor<T> extractor,
                                                     final ResultSetType resultSetType, final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        validateArgumentsTypes(params);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, resultSetType.type(), resultSetType.concurrency())) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            final List<T> results = new ArrayList<>();
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    T item = extractor.extractData(resultSet);
                    results.add(item);
                }
            }

            return Result.success(results);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public <T> CompletableFuture<Result<List<T>, Throwable>> asynchReadListOf(final String sql, final ResultSetExtractor<T> extractor,
                                                                              final @Nullable Object... params) {

        return CompletableFuture.supplyAsync(() -> readListOf(sql, extractor, params));
    }

    public <T> CompletableFuture<Result<List<T>, Throwable>> asynchReadListOf(final String sql,
                                                                              final ResultSetExtractor<T> extractor,
                                                                              final ResultSetType resultSetType,
                                                                              final @Nullable Object... params) {

        return CompletableFuture.supplyAsync(() -> readListOf(sql, extractor, resultSetType, params));
    }

    /**
     * Executes a SQL update (INSERT, UPDATE, DELETE) and manages transactions.
     *
     * @param sql   the SQL update statement to execute
     * @param args  parameters for the SQL statement
     * @return a {@code Result<Integer, Throwable>} with the number of affected rows or an error
     * @throws NullPointerException if {@code sql} or {@code args} is {@code null}
     *
     * <pre>{@code
     * String updateSQL = "UPDATE products SET price = ? WHERE name = ?";
     * Result<Integer, Throwable> result = jetQuerious.write(updateSQL, 24.99, "Sample Product");
     * if (result.success()) {
     *     System.out.println("Rows affected: " + result.value());
     * } else {
     *     System.err.println("Update failed: " + result.throwable().getMessage());
     * }
     * }</pre>
     */
    public Result<Integer, Throwable> write(final String sql, final Object... args) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (args == null) return Result.failure(new IllegalArgumentException("Arguments cannot be null"));
        validateArgumentsTypes(args);

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setParameters(statement, args);
                int affectedRows = statement.executeUpdate();

                connection.commit();
                return Result.success(affectedRows);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            rollback(connection);
            return handleSQLException(e);
        } finally {
            close(connection);
        }
    }

    public CompletableFuture<Result<Integer, Throwable>> asynchWrite(final String sql, final Object... args) {
        return CompletableFuture.supplyAsync(() -> write(sql, args));
    }

    /**
     * Executes a SQL update that includes an array parameter.
     *
     * @param sql             the SQL update statement to execute
     * @param arrayDefinition the SQL type of the array
     * @param arrayIndex      the index of the array parameter in the SQL statement (1-based)
     * @param array           the array to be passed to the SQL statement
     * @param args            additional parameters for the SQL statement
     * @return a {@code Result<Integer, Throwable>} indicating number of affected rows or error
     *
     * <pre>{@code
     * String updateProductTagsSQL = "UPDATE products SET tags = ? WHERE id = ?";
     *
     * String arrayDefinition = "text"; // Assuming the database supports a text array
     * int arrayIndex = 1; // The index of the array parameter in the SQL statement
     * String[] tags = {"electronics", "gadget"};
     * int productId = 1; // The ID of the product to update
     *
     * Result<Integer, Throwable> result = jetQuerious.writeArrayOf(
     *          updateProductTagsSQL,
     *          arrayDefinition,
     *          arrayIndex,
     *          tags,
     *          productId
     * );
     *
     * if (result.success()) {
     *     System.out.println("Rows affected: " + result.value());
     * } else {
     *     System.err.println("Failed to update product tags: " + result.throwable().getMessage());
     * }
     * }</pre>
     */
    public Result<Integer, Throwable> writeArrayOf(final String sql, final String arrayDefinition, final byte arrayIndex,
                                                   final Object[] array, final Object... args) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (arrayDefinition == null) return Result.failure(new IllegalArgumentException("Array definition cannot be null"));
        if (arrayIndex < 1) return Result.failure(new IllegalArgumentException("Array index can`t be below 1"));
        if (array == null) return Result.failure(new IllegalArgumentException("Array cannot be null"));
        if (args == null) return Result.failure(new IllegalArgumentException("Arguments cannot be null"));

        validateArgumentsTypes(args);
        validateArrayDefinition(arrayDefinition);
        validateArrayElementsMatchDefinition(array, arrayDefinition);

        Connection connection = null;
        Array createdArray = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                setParameters(statement, args);
                createdArray = connection.createArrayOf(arrayDefinition, array);
                statement.setArray(arrayIndex, createdArray);
                int affectedRows = statement.executeUpdate();

                connection.commit();
                return Result.success(affectedRows);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            rollback(connection);
            return handleSQLException(e);
        } finally {
            if (createdArray != null) {
                try {
                    createdArray.free();
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, """
                            CRITICAL RESOURCE LEAK: Failed to free SQL Array - this may cause memory leak and system instability.
                            SQL State: %s, Error: %s
                            """.formatted(e.getSQLState(), e.getMessage()), e);
                }
            }
            close(connection);
        }
    }

    public CompletableFuture<Result<Integer, Throwable>> asynchWriteArrayOf(final String sql, final String arrayDefinition,
                                                                            final byte arrayIndex, final Object[] array,
                                                                            final Object... args) {
        return CompletableFuture.supplyAsync(() -> writeArrayOf(sql, arrayDefinition, arrayIndex, array, args));
    }

    /**
     * Executes a batch of SQL updates.
     *
     * @param sql       the SQL update statement to execute
     * @param batchArgs a list of parameter arrays for the batch execution
     * @return a {@code Result<int[], Throwable>} indicating number of affected rows per statement or error
     *
     * <pre>{@code
     * String insertCustomerSQL = "INSERT INTO customers (name, email) VALUES (?, ?)";
     * List<Object[]> batchArgs = Arrays.asList(
     *     new Object[]{"Alice", "alice@example.com"},
     *     new Object[]{"Bob", "bob@example.com"},
     *     new Object[]{"Charlie", "charlie@example.com"}
     * );
     *
     * Result<int[], Throwable> result = jetQuerious.writeBatch(insertCustomerSQL, batchArgs);
     * if (result.success()) {
     *     int[] counts = result.value();
     *     System.out.println("Inserted rows per statement: " + Arrays.toString(counts));
     * } else {
     *     System.err.println("Failed to insert customers: " + result.throwable().getMessage());
     * }
     * }</pre>
     */
    public Result<int[], Throwable> writeBatch(final String sql, final List<Object[]> batchArgs) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (batchArgs == null) return Result.failure(new IllegalArgumentException("Batch arguments cannot be null"));
        for (Object[] params : batchArgs) validateArgumentsTypes(params);

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Object[] args : batchArgs) {
                    setParameters(statement, args);
                    statement.addBatch();
                }
                int[] affectedCounts = statement.executeBatch();

                connection.commit();
                return Result.success(affectedCounts);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            rollback(connection);
            return handleSQLException(e);
        } finally {
            close(connection);
        }
    }

    public CompletableFuture<Result<int[], Throwable>> asynchWriteBatch(final String sql, final List<Object[]> batchArgs) {
        return CompletableFuture.supplyAsync(() -> writeBatch(sql, batchArgs));
    }

    private static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException closeEx) {
                LOG.log(Level.WARNING, "Connection close failed: %s".formatted(closeEx.getMessage()));
            }
        }
    }

    private static void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LOG.log(Level.SEVERE, "Rollback failed: %s".formatted(rollbackEx.getMessage()));
            }
        }
    }

    /**
     * Sets the parameters for a {@code PreparedStatement}.
     *
     * @param statement the {@code PreparedStatement} to set parameters for
     * @param params    the parameters to set
     * @throws SQLException if an SQL error occurs while setting parameters
     */
    private void setParameters(final PreparedStatement statement, final Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            setParameter(statement, param, i);
        }
    }

    private static void setParameter(PreparedStatement statement, Object param, int i) throws SQLException {
        switch (param) {
            case UUID uuid -> statement.setObject(i + 1, uuid.toString());
            case Time time -> statement.setTime(i + 1, time);
            case Timestamp timestamp -> statement.setTimestamp(i + 1, timestamp);
            case LocalDateTime localDateTime -> statement.setObject(i + 1, Timestamp.valueOf(localDateTime));
            case LocalDate localDate -> statement.setObject(i + 1, java.sql.Date.valueOf(localDate));
            case LocalTime localTime -> statement.setObject(i + 1, Time.valueOf(localTime));
            case Instant instant -> statement.setObject(i + 1, Timestamp.from(instant));
            case ZonedDateTime zonedDateTime -> statement.setObject(i + 1, Timestamp.from(zonedDateTime.toInstant()));
            case OffsetDateTime offsetDateTime -> statement.setObject(i + 1, Timestamp.from(offsetDateTime.toInstant()));
            case Duration duration -> statement.setObject(i + 1, duration);
            case Period period -> statement.setObject(i + 1, period);
            case Year year -> statement.setInt(i + 1, year.getValue());
            case YearMonth yearMonth -> statement.setString(i + 1, yearMonth.toString());
            case MonthDay monthDay -> statement.setString(i + 1, monthDay.toString());
            case BigDecimal bigDecimal -> statement.setBigDecimal(i + 1, bigDecimal);
            case BigInteger bigInteger -> statement.setBigDecimal(i + 1, new BigDecimal(bigInteger));
            case Enum<?> enumValue -> statement.setString(i + 1, enumValue.name());
            case URL url -> statement.setURL(i + 1, url);
            case URI uri -> statement.setString(i + 1, uri.toString());
            case Blob blob -> statement.setBlob(i + 1, blob);
            case Clob clob -> statement.setClob(i + 1, clob);
            case byte[] bytes -> statement.setBytes(i + 1, bytes);
            case null -> statement.setNull(i + 1, Types.NULL);
            case String string -> statement.setString(i + 1, string);
            case Byte byteParam -> statement.setByte(i + 1, byteParam);
            case Integer integer -> statement.setInt(i + 1, integer);
            case Short shortParam -> statement.setShort(i + 1, shortParam);
            case Long longParam -> statement.setLong(i + 1, longParam);
            case Float floatParam -> statement.setFloat(i + 1, floatParam);
            case Double doubleParam -> statement.setDouble(i + 1, doubleParam);
            case Boolean booleanParam -> statement.setBoolean(i + 1, booleanParam);
            case Character character -> statement.setObject(i + 1, character);
            default -> {
                Class<?> aClass = param.getClass();
                Field field = FIELDS.get(aClass);

                try {
                    Object value = field.get(param);
                    statement.setObject(i + 1, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(
                            "Could not record the object of class: %s, you must manually specify its mapping"
                                    .formatted(aClass.getName()));
                }
            }
        }
    }

    /**
     * Validates that all parameters are of supported types.
     * Throws an InvalidArgumentTypeException with a detailed message if an unsupported type is found.
     *
     * @param params the parameters to validate
     * @throws InvalidArgumentTypeException if any parameter is of an unsupported type
     */
    private void validateArgumentsTypes(final @Nullable Object... params) {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (!isSupportedType(param)) {
                String className = param.getClass().getName();
                String simpleName = param.getClass().getSimpleName();
                String packageName = param.getClass().getPackage() != null ?
                        param.getClass().getPackage().getName() : "unknown package";

                throw new InvalidArgumentTypeException(
                        String.format("""
                                        Unsupported parameter type at position %d: %s
                                        - Simple class name: %s
                                        - Package: %s
                                        
                                        This library supports the following types:
                                        - Primitives and their wrappers (Integer, Long, Double, etc.)
                                        - String and Character
                                        - Temporal types (LocalDate, LocalDateTime, Instant, ZonedDateTime, etc.)
                                        - Numeric types (BigDecimal, BigInteger)
                                        - UUID, URL, URI
                                        - Enums
                                        - Byte arrays
                                        - Primitive arrays
                                        
                                        To resolve this issue:
                                        1. Convert your object to a supported type before passing it as a parameter
                                        2. Implement a custom type handler in your application
                                        3. Use the specialized methods in this library designed for your data type
                                        4. For collections, flatten them into individual parameters or use batch processing
                                        """,
                                i, className, simpleName, packageName
                        )
                );
            }
        }
    }

    private void validateArrayDefinition(String definition) {
        if (!SUPPORTED_ARRAY_TYPES.containsKey(definition.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("Unsupported array definition: " + definition);
    }

    private void validateArrayElementsMatchDefinition(Object[] array, String definition) {
        Class<?> expectedType = SUPPORTED_ARRAY_TYPES.get(definition.toLowerCase(Locale.ROOT));
        if (expectedType == null)
            throw new IllegalArgumentException("Cannot determine expected type for: " + definition);

        for (Object element : array) {
            if (element != null && !expectedType.isAssignableFrom(element.getClass()))
                throw new IllegalArgumentException("Element '%s' does not match expected type %s"
                        .formatted(element, expectedType.getSimpleName()));
        }
    }

    /**
     * Determines if a parameter is of a type that is directly supported by the setParameters method.
     *
     * @param param the parameter to check
     * @return true if the parameter type is supported, false otherwise
     */
    private boolean isSupportedType(final Object param) {
        return switch (param) {
            case String ignored -> true;
            case Byte ignored -> true;
            case Integer ignored -> true;
            case Short ignored -> true;
            case Long ignored -> true;
            case Float ignored -> true;
            case Double ignored -> true;
            case Boolean ignored -> true;
            case Character ignored -> true;
            case UUID ignored -> true;
            case Time time -> true;
            case Timestamp ignored -> true;
            case LocalDateTime ignored -> true;
            case LocalDate ignored -> true;
            case LocalTime ignored -> true;
            case Instant ignored -> true;
            case ZonedDateTime ignored -> true;
            case OffsetDateTime ignored -> true;
            case Duration ignored -> true;
            case Period ignored -> true;
            case Year ignored -> true;
            case YearMonth ignored -> true;
            case MonthDay ignored -> true;
            case BigDecimal ignored -> true;
            case BigInteger ignored -> true;
            case Enum<?> ignored -> true;
            case URL ignored -> true;
            case URI ignored -> true;
            case Blob ignored -> true;
            case Clob ignored -> true;
            case byte[] ignored -> true;
            case null -> true;
            default -> {
                Class<?> aClass = param.getClass();

                if (FIELDS.containsKey(aClass)) yield true;

                Field[] fields = getDeclaredInstanceFields(aClass);
                if (fields.length != 1) yield false;
                Field field = fields[0];

                try {
                    field.setAccessible(true);

                    Object value = field.get(param);
                    boolean supportedType = isSupportedChildType(value);
                    if (!supportedType) yield false;

                    FIELDS.put(aClass, field);
                    yield true;
                } catch (IllegalAccessException | InaccessibleObjectException | SecurityException e) {
                    yield false;
                }
            }
        };
    }

    private static Field[] getDeclaredInstanceFields(Class<?> clazz) {
        Field[] allFields = clazz.getDeclaredFields();
        return Arrays.stream(allFields)
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .toArray(Field[]::new);
    }

    private boolean isSupportedChildType(Object param) {
        return switch (param) {
            case String ignored -> true;
            case Byte ignored -> true;
            case Integer ignored -> true;
            case Short ignored -> true;
            case Long ignored -> true;
            case Float ignored -> true;
            case Double ignored -> true;
            case Boolean ignored -> true;
            case Character ignored -> true;
            case UUID ignored -> true;
            case Time time -> true;
            case Timestamp ignored -> true;
            case LocalDateTime ignored -> true;
            case LocalDate ignored -> true;
            case LocalTime ignored -> true;
            case Instant ignored -> true;
            case ZonedDateTime ignored -> true;
            case OffsetDateTime ignored -> true;
            case Duration ignored -> true;
            case Period ignored -> true;
            case Year ignored -> true;
            case YearMonth ignored -> true;
            case MonthDay ignored -> true;
            case BigDecimal ignored -> true;
            case BigInteger ignored -> true;
            case Enum<?> ignored -> true;
            case URL ignored -> true;
            case URI ignored -> true;
            case Blob ignored -> true;
            case Clob ignored -> true;
            case byte[] ignored -> true;
            case null -> true;
            default -> false;
        };
    }
}
