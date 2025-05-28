package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.NotFoundException;
import com.hadzhy.jetquerious.exceptions.InvalidArgumentTypeException;
import com.hadzhy.jetquerious.exceptions.TransactionException;
import com.hadzhy.jetquerious.util.Nullable;
import com.hadzhy.jetquerious.util.Result;

import javax.sql.DataSource;
import java.lang.reflect.Field;
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
 * The {@code JetQuerious} class provides a set of utility methods for interacting with a relational database using JetQuerious (Java Database Connectivity).
 * It simplifies the execution of SQL queries and the handling of results, while also providing error handling and transaction management.
 * This class is designed to be used as a singleton within a DI container.
 *
 * <h2>Class API Overview</h2>
 * The {@code JetQuerious} class offers the following key functionalities:
 * <ul>
 *     <li><strong>Read Operations:</strong> Methods to execute SQL queries that return single values, objects, or lists of objects.</li>
 *     <li><strong>Write Operations:</strong> Methods to execute SQL updates, including single updates, updates with array parameters, and batch updates.</li>
 *     <li><strong>Error Handling:</strong> Built-in mechanisms to handle SQL exceptions and translate them into application-specific exceptions.</li>
 *     <li><strong>Transaction Management:</strong> Automatic management of transactions for write status, ensuring data integrity.</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 * To use the {@code JetQuerious} class effectively, follow these guidelines:
 * <ol>
 *     <li><strong>Initialization:</strong> Ensure that the {@code JetQuerious} instance is properly initialized with a valid {@code DataSource} before use.</li>
 *     <li><strong>Read Operations:</strong> Use the {@code read()} method to fetch single values or objects. For complex mappings, consider using the {@code read()} method with a {@code ResultSetExtractor} or {@code RowMapper}.</li>
 *     <li><strong>Write Operations:</strong> Use the {@code write()} method for standard updates. For updates involving arrays, use {@code writeArrayOf()}. For bulk status, utilize {@code writeBatch()}.</li>
 *     <li><strong>Parameter Handling:</strong> Always ensure that parameters passed to SQL statements are properly sanitized and validated to prevent SQL injection attacks.</li>
 *     <li><strong>Error Handling:</strong> Check the result of each status. Use the {@code Result} object to determine success or failure and handle errors appropriately.</li>
 *     <li><strong>Connection Management:</strong> The class manages connections internally, so there is no need to manually open or close connections. However, ensure that the {@code DataSource} is properly configured.</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * // Initialize the JetQuerious instance with a DataSource
 * DataSource dataSource = ...; // Obtain a DataSource instance
 * JetQuerious.init(dataSource);
 * JetQuerious jetQuerious = JetQuerious.instance();
 *
 * // Insert a new product. User static import for SQLBuilder.insert();.
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
 * List<Object[]>batchArgs = Arrays.asList(
 *     new Object[]{"Alice", "alice@example.com"},
 *     new Object[]{"Bob", "bob@example.com"}
 * );
 *
 * Result<Boolean, Throwable> batchResult = jetQuerious.writeBatch(batchInsertSQL, batchArgs);
 * </pre>
 *
 * @author Hadzhyiev Hadzhy
 * @version 2.0
 */
public class JetQuerious {
    private final DataSource dataSource;
    private static volatile JetQuerious instance;
    private static final Map<Class<?>, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Logger LOG = Logger.getLogger(JetQuerious.class.getName());

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
     * Executes an action within a transaction, ensuring atomicity.
     * Automatically manages the database connection: opens a connection,
     * disables auto-commit, executes the provided action, then either commits
     * the changes or rolls them back if an error occurs.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Result<Void, Throwable> result = jetQuerious.transactional(conn -> {
     *     jetQuerious.stepInTransaction(conn, "INSERT INTO users (name) VALUES (?)", "Bob");
     *     jetQuerious.stepInTransaction(conn, "INSERT INTO logs (event) VALUES (?)", "User created");
     * });
     * }</pre>
     *
     * @param action the action to execute within the transaction
     * @return {@code Result.success(null)} on successful execution or {@code Result.failure(throwable)}
     *         on error with the corresponding exception
     *
     * @implNote
     * <ul>
     *   <li>The method automatically closes the connection after the transaction completes</li>
     *   <li>Any exceptions thrown during action execution will cause a transaction rollback</li>
     *   <li>All SQL operations within the transaction must use the same connection passed to the lambda</li>
     *   <li>Use the {@link #stepInTransaction} method for executing operations within the transaction</li>
     * </ul>
     *
     * <h3>⚠️ Critical Warning</h3>
     * <p>
     * <strong>Only operations performed through the {@code stepInTransaction} method will be part of the transaction!</strong>
     * </p>
     * <p>
     * Any other database operations that you might execute inside the {@code transactional} lambda:
     * </p>
     * <ul>
     *   <li>Direct JDBC calls using the connection object</li>
     *   <li>Other database methods that don't use the {@code transaction} method</li>
     *   <li>Custom SQL execution methods</li>
     * </ul>
     * <p>
     * ...will <strong>NOT</strong> be part of the transaction control and will <strong>NOT</strong> be rolled back
     * if an error occurs. They will execute in their default manner without transactional protection.
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
     * @implNote
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
     *         If the operation succeeds, the result is encapsulated in {@link Result#success(T)}.
     *         If an error occurs, it is encapsulated in {@link Result#failure(Throwable)}.
     *
     * @see PreparedStatement
     * @see SQLFunction
     *
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * String sql = "SELECT name, age FROM users WHERE id = ?";
     *
     * int userId = 1
     *
     * Result<List<String>, Throwable> result = jetQuerious.execute(
     *     sql,
     *     statement {
     *         ResultSet resultSet = statement.executeQuery();
     *         List<String> userInfo = new ArrayList<></>();
     *         while (resultSet.next()) {
     *             userInfo.add(resultSet.getString("name"));
     *             userInfo.add(String.valueOf(resultSet.getInt("age")));
     *         }
     *         return userInfo;
     *     },
     *     userId
     * )
     *
     * if (result.isSuccess()) {
     *     List<;String> user = result.get();
     *     System.out.println("User info: " + user);
     * } else {
     *     System.err.println("Error: " + result.getError().getMessage());
     * }
     * </pre>
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
     * @example
     * <pre>
     * Result<UserAccount, Throwable> userResult = jetQuerious.read(
     *          FIND_BY_ID,
     *          this::userAccountMapper,
     *          userId.toString()
     * );
     * </pre>
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
     * @example
     * <pre>
     * Integer count = jetQuerious.readObjectOf(
     *          "SELECT COUNT(email) FROM YOU_TABLE WHERE email = ?",
     *          Integer.class,
     *          verifiableEmail.email()
     * )
     *          .orElseThrow();
     * </pre>
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
     * @example
     * <pre>
     * Result<List<UserAccount>, Throwable> users = jetQuerious.readListOf(
     *          "SELECT * FROM UserAccount",
     *          this::userAccountMapper
     * );
     * </pre>
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
     * @return a {@code Result<Boolean, Throwable>} indicating success or failure
     * @throws NullPointerException if {@code sql} or {@code args} is {@code null}
     *
     * @example
     * <pre>
     * String insertProductSQL = "INSERT INTO products (name, price) VALUES (?, ?)";
     * String productName = "Sample Product";
     * double productPrice = 19.99;
     *
     * Result<Boolean, Throwable> result = jetQuerious.write(insertProductSQL, productName, productPrice);
     * if (result.isSuccess()) {
     *     System.out.println("Product inserted successfully.");
     * } else {
     *     System.err.println("Failed to insert product: " + result.getError().getMessage());
     * }
     * </pre>
     */
    public Result<Boolean, Throwable> write(final String sql, final Object... args) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (args == null) return Result.failure(new IllegalArgumentException("Arguments cannot be null"));
        validateArgumentsTypes(args);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, args);
            statement.executeUpdate();

            return Result.success(Boolean.TRUE);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public CompletableFuture<Result<Boolean, Throwable>> asynchWrite(final String sql, final Object... args) {
        return CompletableFuture.supplyAsync(() -> write(sql, args));
    }

    /**
     * Executes a SQL update that includes an array parameter.
     *
     * @param sql             the SQL update statement to execute
     * @param arrayDefinition the SQL type of the array
     * @param arrayIndex      the index of the array parameter in the SQL statement
     * @param array           the array to be passed to the SQL statement
     * @param args            additional parameters for the SQL statement
     * @return a {@code Result<Boolean, Throwable>} indicating success or failure
     *
     * @example
     * <pre>
     * String updateProductTagsSQL = "UPDATE products SET tags = ? WHERE id = ?";
     *
     * String arrayDefinition = "text"; // Assuming the database supports a text array
     * int arrayIndex = 1; // The index of the array parameter in the SQL statement
     * String[] tags = {"electronics", "gadget"};
     * int productId = 1; // The ID of the product to update
     *
     * Result<Boolean, Throwable> result = jetQuerious.writeArrayOf(
     *          updateProductTagsSQL,
     *          arrayDefinition,
     *          arrayIndex,
     *          tags,
     *          productId
     * );
     *
     * if (result.isSuccess()) {
     *     System.out.println("Product tags updated successfully.");
     * } else {
     *     System.err.println("Failed to update product tags: " + result.getError().getMessage());
     * }
     * </pre>
     */
    public Result<Boolean, Throwable> writeArrayOf(final String sql, final String arrayDefinition, final byte arrayIndex,
                                                   final Object[] array, final Object... args) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (arrayDefinition == null) return Result.failure(new IllegalArgumentException("Array definition cannot be null"));
        if (array == null) return Result.failure(new IllegalArgumentException("Array cannot be null"));
        if (args == null) return Result.failure(new IllegalArgumentException("Arguments cannot be null"));
        validateArgumentsTypes(args);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, args);
            final Array createdArray = connection.createArrayOf(arrayDefinition, array);
            statement.setArray(arrayIndex, createdArray);
            statement.executeUpdate();

            return Result.success(true);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public CompletableFuture<Result<Boolean, Throwable>> asynchWriteArrayOf(final String sql, final String arrayDefinition,
                                                                            final byte arrayIndex, final Object[] array,
                                                                            final Object... args) {

        return CompletableFuture.supplyAsync(() -> writeArrayOf(sql, arrayDefinition, arrayIndex, array, args));
    }

    /**
     * Executes a batch of SQL updates.
     *
     * @param sql       the SQL update statement to execute
     * @param batchArgs a list of parameter arrays for the batch execution
     * @return a {@code Result<Boolean, Throwable>} indicating success or failure
     *
     * @example
     * <pre>
     * String insertCustomerSQL = "INSERT INTO customers (name, email) VALUES (?, ?)";
     * List<Object[]> batchArgs = Arrays.asList(
     *     new Object[]{"Alice", "alice@example.com"},
     *     new Object[]{"Bob", "bob@example.com"},
     *     new Object[]{"Charlie", "charlie@example.com"}
     * );
     *
     * Result<Boolean, Throwable> result = jetQuerious.writeBatch(insertCustomerSQL, batchArgs);
     * if (result.isSuccess()) {
     *     System.out.println("Customers inserted successfully.");
     * } else {
     *     System.err.println("Failed to insert customers: " + result.getError().getMessage());
     * }
     * </pre>
     */
    public Result<Boolean, Throwable> writeBatch(final String sql, final List<Object[]> batchArgs) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (batchArgs == null) return Result.failure(new IllegalArgumentException("Batch arguments cannot be null"));
        for (Object[] params : batchArgs) validateArgumentsTypes(params);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Object[] args : batchArgs) {
                setParameters(statement, args);
                statement.addBatch();
            }

            statement.executeBatch();

            return Result.success(true);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public CompletableFuture<Result<Boolean, Throwable>> asynchWriteBatch(final String sql, final List<Object[]> batchArgs) {
        return CompletableFuture.supplyAsync(() -> writeBatch(sql, batchArgs));
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

                Field[] fields = aClass.getFields();
                if (fields.length != 1) yield false;
                Field field = fields[0];
                field.setAccessible(true);
                try {
                    Object value = field.get(param);
                    boolean supportedType = isSupportedChildType(value);
                    if (!supportedType) yield false;

                    FIELDS.put(aClass, field);
                    yield true;
                } catch (IllegalAccessException e) {
                    yield false;
                }
            }
        };
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
