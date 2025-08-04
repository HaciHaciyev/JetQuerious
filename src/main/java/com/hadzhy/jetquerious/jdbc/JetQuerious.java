package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.NotFoundException;
import com.hadzhy.jetquerious.asynch.BatchErrorHandler;
import com.hadzhy.jetquerious.asynch.JetQExecutor;
import com.hadzhy.jetquerious.exceptions.InvalidArgumentTypeException;
import com.hadzhy.jetquerious.exceptions.TransactionException;
import com.hadzhy.jetquerious.util.Nullable;
import com.hadzhy.jetquerious.util.Result;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hadzhy.jetquerious.jdbc.SQLErrorHandler.handleSQLException;

/**
 * The {@code JetQuerious} class provides a set of utility methods for
 * interacting with a relational database using JDBC (Java Database
 * Connectivity).
 * It simplifies the execution of SQL queries and the handling of results, while
 * also providing error handling and transaction management.
 * This class is designed to be used as a singleton within a DI container.
 *
 * <h2>Class API Overview</h2>
 * The {@code JetQuerious} class offers the following key functionalities:
 * <ul>
 * <li><strong>Read Operations:</strong> Methods to execute SQL queries that
 * return single values, objects, or lists of objects.</li>
 * <li><strong>Write Operations:</strong> Methods to execute SQL updates,
 * including single updates, updates with array parameters, and batch
 * updates.</li>
 * <li><strong>Error Handling:</strong> Built-in mechanisms to handle SQL
 * exceptions and translate them into application-specific exceptions.</li>
 * <li><strong>Transaction Management:</strong> Automatic management of
 * transactions for write operations, ensuring data integrity.</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 * To use the {@code JetQuerious} class effectively, follow these guidelines:
 * <ol>
 * <li><strong>Initialization:</strong> Ensure that the {@code JetQuerious}
 * instance is properly initialized with a valid {@code DataSource} before
 * use.</li>
 * <li><strong>Read Operations:</strong> Use the {@code read()} method to fetch
 * single values or objects. For complex mappings, consider using the
 * {@code read()} method with a {@code ResultSetExtractor} or
 * {@code RowMapper}.</li>
 * <li><strong>Write Operations:</strong> Use the {@code write()} method for
 * standard updates. For updates involving arrays, use {@code writeArrayOf()}.
 * For bulk operations, utilize {@code writeBatch()}.</li>
 * <li><strong>Parameter Handling:</strong> Always ensure that parameters passed
 * to SQL statements are properly sanitized and validated to prevent SQL
 * injection attacks.</li>
 * <li><strong>Error Handling:</strong> Check the result of each operation. Use
 * the {@code Result} object to determine success or failure and handle errors
 * appropriately.</li>
 * <li><strong>Connection Management:</strong> The class manages connections
 * internally, so there is no need to manually open or close connections.
 * However, ensure that the {@code DataSource} is properly configured.</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * 
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
    private DataSource dataSource;
    private JetQExecutor executor;
    private static JetQuerious instance;
    private static final Logger LOG = Logger.getLogger(JetQuerious.class.getName());

    private JetQuerious(DataSource dataSource, JetQExecutor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    public static synchronized void init(DataSource dataSource) {
        if (instance == null)
            instance = new JetQuerious(dataSource, new JetQExecutor());
    }

    public static synchronized void init(DataSource dataSource, int queueCapacity, int batchSize) {
        if (instance == null)
            instance = new JetQuerious(dataSource, new JetQExecutor(queueCapacity, batchSize, null));
    }

    public static synchronized void init(
            DataSource dataSource,
            int queueCapacity,
            int batchSize,
            BatchErrorHandler batchErrorHandler) {

        if (instance == null)
            instance = new JetQuerious(dataSource, new JetQExecutor(queueCapacity, batchSize, batchErrorHandler));
    }

    public synchronized void setDatasource(DataSource updatedDataSource) {
        if (updatedDataSource == null)
            throw new IllegalArgumentException("Data source cannot be null.");
        this.dataSource = updatedDataSource;
    }

    public static JetQuerious instance() {
        if (instance == null)
            throw new IllegalStateException("JetQuerious is not initialized. Call JDBC.init(dataSource) first.");
        return instance;
    }

    /**
     * Gracefully shutdown the async executor and JetQuerious instance.
     * This method should be called during application shutdown.
     * 
     * @param timeout maximum time to wait for tasks to complete
     * @param unit    time unit for the timeout
     * @return CompletableFuture that completes when shutdown is finished
     */
    public static synchronized CompletableFuture<Void> shutdown(long timeout, TimeUnit unit) {
        if (instance != null && instance.executor != null) {
            return instance.executor.shutdownGracefully(timeout, unit)
                    .thenRun(() -> {
                        instance = null;
                        LOG.info("JetQuerious shutdown completed");
                    });
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Immediate shutdown without waiting for task completion
     */
    public static synchronized void shutdownNow() {
        if (instance != null && instance.executor != null) {
            instance.executor.shutdown();
            instance = null;
            LOG.info("JetQuerious immediate shutdown completed");
        }
    }

    public synchronized void restartAsynchExecutor() {
        if (this.executor == null)
            this.executor = new JetQExecutor();
    }

    public synchronized void restartAsynchExecutor(int queueCapacity, int batchSize,
            BatchErrorHandler batchErrorHandler) {
        if (this.executor == null)
            this.executor = new JetQExecutor(queueCapacity, batchSize, batchErrorHandler);
    }

    /**
     * Executes the given action within a single database transaction, ensuring
     * atomicity.
     * Automatically manages the connection lifecycle: it opens a connection,
     * disables auto-commit,
     * executes the action, and then either commits the changes or rolls them back
     * in case of an error.
     *
     * <p>
     * <b>Example usage:</b>
     * </p>
     * 
     * <pre>{@code
     * Result<Void, Throwable> result = jetQuerious.transactional(conn -> {
     *     jetQuerious.stepInTransaction(conn, "INSERT INTO users (name) VALUES (?)", "Bob");
     *     jetQuerious.stepInTransaction(conn, "INSERT INTO logs (event) VALUES (?)", "User created");
     * });
     * }</pre>
     *
     * @param action the database operations to perform within the transaction,
     *               receiving a {@link Connection} object
     * @return {@code Result.success(null)} if the transaction was committed
     *         successfully;
     *         otherwise, {@code Result.failure(throwable)} with the exception that
     *         caused rollback
     *
     *         <p>
     *         <b>Note:</b>
     *         </p>
     *         <ul>
     *         <li>This method automatically closes the connection when the
     *         transaction completes.</li>
     *         <li>If the action throws an exception, the transaction is rolled
     *         back.</li>
     *         <li>All operations within the transaction must use the provided
     *         {@code Connection} object.</li>
     *         <li>Use {@link #stepInTransaction} for executing SQL within the
     *         transaction context.</li>
     *         </ul>
     *
     *         <p>
     *         <b>⚠ Important:</b>
     *         </p>
     *         <p>
     *         Only operations performed via {@code stepInTransaction} will be part
     *         of the transaction.
     *         Avoid executing raw JDBC calls or using other data access methods
     *         within the transactional lambda,
     *         unless you are certain they respect the same connection and
     *         transaction context.
     *         </p>
     *         <p>
     *         Operations that are <b>not</b> part of the transaction include:
     *         </p>
     *         <ul>
     *         <li>Direct JDBC calls using the connection object</li>
     *         <li>Calls to methods that do not use {@code stepInTransaction}</li>
     *         <li>Custom SQL execution utilities not designed for transactional
     *         use</li>
     *         </ul>
     *         <p>
     *         These operations will execute independently and <b>will not</b> be
     *         rolled back
     *         if the transaction fails.
     *         </p>
     */
    public Result<Void, Throwable> transactional(SQLConsumer<Connection> action) {
        if (action == null)
            return Result.failure(new IllegalArgumentException("Action must not be null"));

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            return commitTransaction(action, connection);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Exception during transaction", t);
            return Result.failure(t);
        }
    }

    private static Result<Void, Throwable> commitTransaction(SQLConsumer<Connection> action,
            Connection connection) throws SQLException {
        try {
            action.accept(connection);
            connection.commit();
            return Result.success(null);
        } catch (Throwable t) {
            connection.rollback();
            return Result.failure(t);
        }
    }

    /**
     * Executes a single SQL update operation using a provided connection and
     * parameters.
     * <p>
     * This method is intended for internal use within transactional blocks managed
     * by
     * {@link #transactional(SQLConsumer)}. It prepares and executes the given SQL
     * statement
     * with the provided parameters using the given JDBC connection.
     * <p>
     * The method does not handle connection management or transactional boundaries
     * itself.
     * It relies on the caller to ensure the connection is part of a valid
     * transaction and
     * properly managed.
     * <p>
     * Usage is expected only via the lambda passed to {@code transactional()}, for
     * example:
     *
     * <pre>{@code
     * jetQuerious.transactional(conn -> {
     *     jetQuerious.stepInTransaction(conn, "UPDATE accounts SET balance = balance - ? WHERE id = ?", 100, 1);
     *     jetQuerious.stepInTransaction(conn, "UPDATE accounts SET balance = balance + ? WHERE id = ?", 100, 2);
     * });
     * }</pre>
     *
     * @param connection the JDBC connection to use, must be managed externally
     *                   (e.g., from {@code transactional()})
     * @param sql        the SQL update statement to execute (must not be null)
     * @param params     optional parameters to bind to the prepared statement
     *
     * @throws IllegalArgumentException if the connection or sql is null
     * @throws TransactionException     if a database access error occurs during
     *                                  execution
     *
     *                                  <p>
     *                                  <b>Note:</b>
     *                                  </p>
     *                                  - This method does not perform any
     *                                  transaction commit or rollback.
     *                                  - It should never be called outside the
     *                                  context of {@code transactional()}.
     *                                  - Exceptions are thrown directly to
     *                                  propagate transactional failure.
     */
    public void stepInTransaction(final Connection connection, final String sql,
            final @Nullable Object... params) {

        if (connection == null)
            throw new IllegalArgumentException("Connection cannot be null");
        if (sql == null)
            throw new IllegalArgumentException("SQL cannot be null");
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
     * Executes a custom SQL operation using the provided SQL query and a callback
     * function
     * to operate on a {@link PreparedStatement}. The callback allows the user to
     * control
     * the execution logic (e.g., executeQuery, executeUpdate) and handle the
     * result.
     *
     * <p>
     * The method ensures that the connection and PreparedStatement are closed after
     * execution,
     * and it provides a safe way to handle SQL exceptions.
     * </p>
     *
     * @param <T>      The type of result returned by the callback function.
     * @param sql      The SQL query string to be executed.
     * @param callback The callback function that will operate on the
     *                 {@link PreparedStatement}.
     *                 It can execute queries, updates, or batch operations, and
     *                 return a result.
     * @param params   The parameters to be set in the PreparedStatement.
     *                 They are set in the order they appear in the SQL query.
     *
     * @return A {@link Result} object containing the result of the callback
     *         execution or an error if the operation fails.
     *         If the operation succeeds, the result is encapsulated in
     *         {@code Result.success(T)}.
     *         If an error occurs, it is encapsulated in
     *         {@code Result.failure(Throwable)}.
     *
     * @see PreparedStatement
     * @see SQLFunction
     *
     *      <p>
     *      <b>Example usage:</b>
     *      </p>
     *
     *      <pre>{@code
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
     *      }
     *      return userInfo;
     *      },
     *      userId);
     *
     *      if (result.isSuccess()) {
     *      List&lt;String&gt; user = result.get();
     *      System.out.println("User info: " + user);
     *      } else {
     *      System.err.println("Error: " + result.getError().getMessage());
     *      }
     * }</pre>
     */
    public <T> Result<T, Throwable> execute(
            String sql,
            SQLFunction<PreparedStatement, T> callback,
            @Nullable Object... params) {

        try {
            return Result.success(doExecute(sql, callback, params));
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Exception during custom execute", e);
            return switch (e) {
                case SQLException sqlException -> handleSQLException(sqlException);
                default -> Result.failure(e);
            };
        }
    }

    public <T> CompletionStage<T> asynchExecute(
            String sql,
            SQLFunction<PreparedStatement, T> callback,
            @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doExecute(sql, callback, params);
            } catch (Throwable e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(handleSQLException(sqlException).throwable());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private <T> T doExecute(
            String sql,
            SQLFunction<PreparedStatement, T> callback,
            @Nullable Object... params) throws Throwable {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (callback == null)
            throw new IllegalArgumentException("Callback function cannot be null");

        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.success())
            throw typesResult.throwable();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, params);
            return callback.apply(statement);
        }
    }

    /**
     * Executes a SQL query and uses a {@code ResultSetExtractor} to map the result
     * set to an object.
     *
     * @param sql       the SQL query to execute
     * @param extractor a functional interface for extracting data from the
     *                  {@code ResultSet}
     * @param params    optional parameters for the SQL query
     * @param <T>       the type of the extracted object
     * @return a {@code Result<T, Throwable>} containing the extracted object or an
     *         error
     * @throws NullPointerException if {@code sql} or {@code extractor} is
     *                              {@code null}
     *
     *                              <pre>{@code
     * Result<UserAccount, Throwable> userResult = jetQuerious.read(
     *          FIND_BY_ID,
     *          this::userAccountMapper,
     *          userId.toString()
     * );
     * }</pre>
     */
    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        try {
            return Result.success(doRead(sql, extractor, params));
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return switch (e) {
                case SQLException sqlException -> handleSQLException(sqlException);
                default -> Result.failure(e);
            };
        }
    }

    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) {

        try {
            return Result.success(doRead(sql, extractor, resultSetType, params));
        } catch (Throwable e) {
            return switch (e) {
                case SQLException sqlException -> handleSQLException(sqlException);
                default -> Result.failure(e);
            };
        }
    }

    public <T> CompletableFuture<T> asynchRead(final String sql,
            final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doRead(sql, extractor, params);
            } catch (Throwable e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(handleSQLException(sqlException).throwable());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    public <T> CompletableFuture<T> asynchRead(final String sql,
            final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doRead(sql, extractor, resultSetType, params);
            } catch (Throwable e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(handleSQLException(sqlException).throwable());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    public <T> T doRead(final String sql, final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) throws Throwable {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (extractor == null)
            throw new IllegalArgumentException("Extractor cannot be null");
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.success())
            throw typesResult.throwable();

        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Data in for this query was not found.");
                }

                T value = extractor.extractData(resultSet);
                return value;
            }
        }
    }

    public <T> T doRead(final String sql, final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) throws Throwable {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (extractor == null)
            throw new IllegalArgumentException("Extractor cannot be null");
        if (resultSetType == null)
            throw new IllegalArgumentException("Result set type can`t be null");
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.success())
            throw typesResult.throwable();

        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql, resultSetType.type(),
                        resultSetType.concurrency())) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Data in for this query was not found.");
                }

                T value = extractor.extractData(resultSet);
                return value;
            }
        }
    }

    /**
     * Executes a SQL query that returns a single value of a specified wrapper type.
     *
     * @param sql    the SQL query to execute
     * @param type   the expected return type, which must be a wrapper type (e.g.,
     *               Integer.class)
     * @param params optional parameters for the SQL query
     * @param <T>    the type of the result
     * @return a {@code Result<T, Throwable>} containing the result or an error
     * @throws NullPointerException         if {@code sql} or {@code type} is
     *                                      {@code null}
     * @throws InvalidArgumentTypeException if the specified type is not a valid
     *                                      wrapper type
     *
     *                                      <pre>{@code
     * Integer count = jetQuerious.readObjectOf(
     *          "SELECT COUNT(email) FROM YOU_TABLE WHERE email = ?",
     *          Integer.class,
     *          verifiableEmail.email()
     * )
     *          .orElseThrow();
     * }</pre>
     */
    public <T> Result<T, Throwable> readObjectOf(final String sql, final Class<T> type,
            final @Nullable Object... params) {

        try {
            return Result.success(doReadObjectOf(sql, type, params));
        } catch (Throwable e) {
            return switch (e) {
                case SQLException sqlException -> Result.failure(handleSQLException(sqlException).throwable());
                default -> Result.failure(e);
            };
        }
    }

    public <T> CompletableFuture<T> asynchReadObjectOf(final String sql, final Class<T> type,
            final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doReadObjectOf(sql, type, params);
            } catch (Throwable e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(handleSQLException(sqlException).throwable());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    public <T> T doReadObjectOf(final String sql, final Class<T> type,
                                final @Nullable Object... params) throws Throwable {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (type == null)
            throw new IllegalArgumentException("Type cannot be null");
        for (Object param : params) {
            if (param instanceof Enum<?>)
                throw new InvalidArgumentTypeException(
                        "Enum conversion is not supported directly. Use specific enum type or handle separately.");
        }
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.success())
            throw typesResult.throwable();

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Data in query for object was not found.");
                }

                T value = Mapper.map(resultSet, type);
                return value;
            }
        }
    }

    /**
     * Executes a SQL query that returns a list of objects mapped by a
     * {@code RowMapper}.
     *
     * @param sql       the SQL query to execute
     * @param extractor a functional interface for mapping rows of the result set to
     *                  objects
     * @param <T>       the type of the mapped objects
     * @return a {@code Result<List<T>, Throwable>} containing the list of mapped
     *         objects or an error
     * @throws NullPointerException if {@code sql} or {@code rowMapper} is
     *                              {@code null}
     *
     *                              <pre>{@code
     * Result<List<UserAccount>, Throwable> users = jetQuerious.readListOf(
     *          "SELECT * FROM UserAccount",
     *          this::userAccountMapper
     * );
     * }</pre>
     */
    public <T> Result<List<T>, Throwable> readListOf(final String sql, final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        if (sql == null)
            return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null)
            return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.success())
            return Result.failure(typesResult.throwable());

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

        if (sql == null)
            return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null)
            return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.success())
            return Result.failure(typesResult.throwable());

        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql, resultSetType.type(),
                        resultSetType.concurrency())) {
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

    public <T> CompletableFuture<List<T>> asynchReadListOf(final String sql,
            final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        CompletableFuture<List<T>> future = new CompletableFuture<>();

        executor.execute(() -> {
            Result<List<T>, Throwable> result = readListOf(sql, extractor, params);
            if (result.success())
                future.complete(result.value());
            else
                future.completeExceptionally(result.throwable());
            return null;
        });

        return future;
    }

    public <T> CompletableFuture<List<T>> asynchReadListOf(final String sql,
            final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType,
            final @Nullable Object... params) {

        CompletableFuture<List<T>> future = new CompletableFuture<>();

        executor.execute(() -> {
            Result<List<T>, Throwable> result = readListOf(sql, extractor, resultSetType, params);
            if (result.success())
                future.complete(result.value());
            else
                future.completeExceptionally(result.throwable());
            return null;
        });

        return future;
    }

    /**
     * Executes a SQL update (INSERT, UPDATE, DELETE) and manages transactions.
     *
     * @param sql  the SQL update statement to execute
     * @param args parameters for the SQL statement
     * @return a {@code Result<Integer, Throwable>} with the number of affected rows
     *         or an error
     * @throws NullPointerException if {@code sql} or {@code args} is {@code null}
     *
     *                              <pre>{@code
     * String updateSQL = "UPDATE products SET price = ? WHERE name = ?";
     * Result<Integer, Throwable> result = jetQuerious.write(updateSQL, 24.99, "Sample Product");
     * if (result.success()) {
     *     System.out.println("Rows affected: " + result.value());
     *                              } else {
     *                              System.err.println("Update failed: " + result.throwable().getMessage());
     *                              }
     * }</pre>
     */
    public Result<Integer, Throwable> write(final String sql, final Object... args) {
        if (sql == null)
            return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (args == null)
            return Result.failure(new IllegalArgumentException("Arguments cannot be null"));
        var typesResult = validateArgumentsTypes(args);
        if (!typesResult.success())
            return Result.failure(typesResult.throwable());

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

    public CompletableFuture<Integer> asynchWrite(final String sql, final Object... args) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        executor.execute(() -> {
            Result<Integer, Throwable> result = write(sql, args);
            if (result.success())
                future.complete(result.value());
            else
                future.completeExceptionally(result.throwable());
            return null;
        });

        return future;
    }

    /**
     * Executes a SQL update that includes an array parameter.
     *
     * @param sql             the SQL update statement to execute
     * @param arrayDefinition the SQL type of the array
     * @param arrayIndex      the index of the array parameter in the SQL statement
     *                        (1-based)
     * @param array           the array to be passed to the SQL statement
     * @param args            additional parameters for the SQL statement
     * @return a {@code Result<Integer, Throwable>} indicating number of affected
     *         rows or error
     *
     *         <pre>{@code
     * String updateProductTagsSQL = "UPDATE products SET tags = ? WHERE id = ?";
     *
     * String arrayDefinition = "text"; // Assuming the database supports a text array
     * int arrayIndex = 1; // The index of the array parameter in the SQL statement
     *         String[] tags = { "electronics", "gadget" };
     *         int productId = 1; // The ID of the product to update
     *
     *         Result<Integer, Throwable> result = jetQuerious.writeArrayOf(
     *         updateProductTagsSQL,
     *         arrayDefinition,
     *         arrayIndex,
     *         tags,
     *         productId);
     *
     *         if (result.success()) {
     *         System.out.println("Rows affected: " + result.value());
     *         } else {
     *         System.err.println("Failed to update product tags: " + result.throwable().getMessage());
     *         }
     * }</pre>
     */
    public Result<Integer, Throwable> writeArrayOf(final String sql, final String arrayDefinition,
            final byte arrayIndex,
            final Object[] array, final Object... args) {
        if (sql == null)
            return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (arrayDefinition == null)
            return Result.failure(new IllegalArgumentException("Array definition cannot be null"));
        if (arrayIndex < 1)
            return Result.failure(new IllegalArgumentException("Array index can`t be below 1"));
        if (array == null)
            return Result.failure(new IllegalArgumentException("Array cannot be null"));
        if (args == null)
            return Result.failure(new IllegalArgumentException("Arguments cannot be null"));

        var typesResult = validateArgumentsTypes(args);
        if (!typesResult.success())
            return Result.failure(typesResult.throwable());

        var arrayRes = validateArray(array, arrayDefinition);
        if (!arrayRes.success())
            return Result.failure(arrayRes.throwable());

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
                    LOG.log(Level.SEVERE,
                            """
                                    CRITICAL RESOURCE LEAK: Failed to free SQL Array - this may cause memory leak and system instability.
                                    SQL State: %s, Error: %s
                                    """
                                    .formatted(e.getSQLState(), e.getMessage()),
                            e);
                }
            }
            close(connection);
        }
    }

    public CompletableFuture<Integer> asynchWriteArrayOf(final String sql,
            final String arrayDefinition,
            final byte arrayIndex, final Object[] array,
            final Object... args) {

        CompletableFuture<Integer> future = new CompletableFuture<>();

        executor.execute(() -> {
            Result<Integer, Throwable> result = writeArrayOf(sql, arrayDefinition, arrayIndex, array, args);
            if (result.success())
                future.complete(result.value());
            else
                future.completeExceptionally(result.throwable());
            return null;
        });

        return future;
    }

    /**
     * Executes a batch of SQL updates.
     *
     * @param sql       the SQL update statement to execute
     * @param batchArgs a list of parameter arrays for the batch execution
     * @return a {@code Result<int[], Throwable>} indicating number of affected rows
     *         per statement or error
     *
     *         <pre>{@code
     * String insertCustomerSQL = "INSERT INTO customers (name, email) VALUES (?, ?)";
     * List<Object[]> batchArgs = Arrays.asList(
     *         new Object[] { "Alice", "alice@example.com" },
     *         new Object[] { "Bob", "bob@example.com" },
     *         new Object[] { "Charlie", "charlie@example.com" });
     *
     *         Result<int[], Throwable> result = jetQuerious.writeBatch(insertCustomerSQL, batchArgs);
     *         if (result.success()) {
     *         int[] counts = result.value();
     *         System.out.println("Inserted rows per statement: " + Arrays.toString(counts));
     *         } else {
     *         System.err.println("Failed to insert customers: " + result.throwable().getMessage());
     *         }
     * }</pre>
     */
    public Result<int[], Throwable> writeBatch(final String sql, final List<Object[]> batchArgs) {
        if (sql == null)
            return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (batchArgs == null)
            return Result.failure(new IllegalArgumentException("Batch arguments cannot be null"));
        for (Object[] params : batchArgs) {
            var typesResult = validateArgumentsTypes(params);
            if (!typesResult.success())
                return Result.failure(typesResult.throwable());
        }

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

    public CompletableFuture<int[]> asynchWriteBatch(final String sql,
            final List<Object[]> batchArgs) {

        CompletableFuture<int[]> future = new CompletableFuture<>();

        executor.execute(() -> {
            Result<int[], Throwable> result = writeBatch(sql, batchArgs);
            if (result.success())
                future.complete(result.value());
            else
                future.completeExceptionally(result.throwable());
            return null;
        });

        return future;
    }

    private static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException closeEx) {
                LOG.log(Level.SEVERE, "Connection close failed: %s".formatted(closeEx.getMessage()));
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

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneakyThrow(Throwable e) throws E {
        throw (E) e;
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
            ParameterSetter.setParameter(statement, param, i + 1);
        }
    }

    private Result<Void, Throwable> validateArray(Object[] array, String arrayDefinition) {
        try {
            TypeRegistry.validateArrayDefinition(arrayDefinition);
            TypeRegistry.validateArrayElementsMatchDefinition(array, arrayDefinition);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.failure(new InvalidArgumentTypeException(e.getMessage()));
        }
    }

    private Result<Void, InvalidArgumentTypeException> validateArgumentsTypes(final @Nullable Object... params) {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (!TypeRegistry.isSupportedType(param)) {
                String className = param.getClass().getName();
                String simpleName = param.getClass().getSimpleName();
                String packageName = param.getClass().getPackage() != null ? param.getClass().getPackage().getName()
                        : "unknown package";

                return Result.failure(new InvalidArgumentTypeException(
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
                                i, className, simpleName, packageName)));
            }
        }

        return Result.success(null);
    }
}
