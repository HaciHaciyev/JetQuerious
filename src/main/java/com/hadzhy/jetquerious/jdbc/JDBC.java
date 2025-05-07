package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.exceptions.NotFoundException;
import com.hadzhy.jetquerious.exceptions.InvalidArgumentTypeException;
import com.hadzhy.jetquerious.util.Nullable;
import com.hadzhy.jetquerious.util.Result;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hadzhy.jetquerious.jdbc.SQLErrorHandler.handleSQLException;

/**
 * The {@code JDBC} class provides a set of utility methods for interacting with a relational database using JDBC (Java Database Connectivity).
 * It simplifies the execution of SQL queries and the handling of results, while also providing error handling and transaction management.
 * This class is designed to be used as a singleton within a DI container.
 *
 * <h2>Class API Overview</h2>
 * The {@code JDBC} class offers the following key functionalities:
 * <ul>
 *     <li><strong>Read Operations:</strong> Methods to execute SQL queries that return single values, objects, or lists of objects.</li>
 *     <li><strong>Write Operations:</strong> Methods to execute SQL updates, including single updates, updates with array parameters, and batch updates.</li>
 *     <li><strong>Error Handling:</strong> Built-in mechanisms to handle SQL exceptions and translate them into application-specific exceptions.</li>
 *     <li><strong>Transaction Management:</strong> Automatic management of transactions for write status, ensuring data integrity.</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 * To use the {@code JDBC} class effectively, follow these guidelines:
 * <ol>
 *     <li><strong>Initialization:</strong> Ensure that the {@code JDBC} instance is properly initialized with a valid {@code DataSource} before use.</li>
 *     <li><strong>Read Operations:</strong> Use the {@code read()} method to fetch single values or objects. For complex mappings, consider using the {@code read()} method with a {@code ResultSetExtractor} or {@code RowMapper}.</li>
 *     <li><strong>Write Operations:</strong> Use the {@code write()} method for standard updates. For updates involving arrays, use {@code writeArrayOf()}. For bulk status, utilize {@code writeBatch()}.</li>
 *     <li><strong>Parameter Handling:</strong> Always ensure that parameters passed to SQL statements are properly sanitized and validated to prevent SQL injection attacks.</li>
 *     <li><strong>Error Handling:</strong> Check the result of each status. Use the {@code Result} object to determine success or failure and handle errors appropriately.</li>
 *     <li><strong>Connection Management:</strong> The class manages connections internally, so there is no need to manually open or close connections. However, ensure that the {@code DataSource} is properly configured.</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * // Initialize the JDBC instance with a DataSource
 * DataSource dataSource = ...; // Obtain a DataSource instance
 * JDBC.init(dataSource);
 * JDBC jdbc = JDBC.instance();
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
 * Result<Boolean, Throwable> insertResult = jdbc.write(insertSQL, "New Product", 29.99);
 *
 * // Fetch a product by ID
 * String selectSQL = select()
 *      .all()
 *      .from("products")
 *      .where("id = ?")
 *      .build()
 *      .sql();
 *
 * Result<Product, Throwable> productResult = jdbc.read(selectSQL, Product.class, 1);
 *
 * // Update product tags
 * String updateTagsSQL = update("products")
 *      .set("tags = ?")
 *      .where("id = ?")
 *      .build()
 *      .sql();
 *
 * String[] tags = {"electronics", "sale"};
 * Result<Boolean, Throwable> updateResult = jdbc.writeArrayOf(updateTagsSQL, "text", 1, tags, 1);
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
 * Result<Boolean, Throwable> batchResult = jdbc.writeBatch(batchInsertSQL, batchArgs);
 * </pre>
 *
 * @author Hadzhyiev Hadzhy
 * @version 2.0
 */
public class JDBC {
    private final DataSource dataSource;
    private static volatile JDBC instance;
    private static final Logger Log = Logger.getLogger(JDBC.class.getName());

    private JDBC(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static void init(DataSource dataSource) {
        if (instance == null) synchronized (JDBC.class) {
            if (instance == null) instance = new JDBC(dataSource);
        }
    }

    public static JDBC instance() {
        if (instance == null)
            throw new IllegalStateException("JDBC is not initialized. Call JDBC.init(dataSource) first.");
        return instance;
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
     * Result<List<String>, Throwable> result = jdbc.execute(
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

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, params);

            T result = callback.apply(statement);
            return Result.success(result);
        } catch (SQLException e) {
            Log.log(Level.SEVERE, "Exception during custom execute", e);
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
     * Result<UserAccount, Throwable> userResult = jdbc.read(
     *          FIND_BY_ID,
     *          this::userAccountMapper,
     *          userId.toString()
     * );
     * </pre>
     */
    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor, final @Nullable Object... params) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null && params.length > 0) {
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
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor,
                                         final ResultSetType resultSetType, final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));
        if (resultSetType == null) return Result.failure(new IllegalArgumentException("Result set type can`t be null"));

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, resultSetType.type(), resultSetType.concurrency())) {
            if (params != null && params.length > 0) {
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
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
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
     * Integer count = jdbc.readObjectOf(
     *          "SELECT COUNT(email) FROM YOU_TABLE WHERE email = ?",
     *          Integer.class,
     *          verifiableEmail.email()
     * )
     *          .orElseThrow();
     * </pre>
     */
    public <T> Result<T, Throwable> readObjectOf(final String sql, final Class<T> type, @Nullable final Object... params) {
        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (type == null) return Result.failure(new IllegalArgumentException("Type cannot be null"));

        final boolean isWrapper = WrappersMapper.isSupportedWrapperType(type);
        if (!isWrapper) {
            return Result.failure(
                    new InvalidArgumentTypeException("Invalid class type. Function jdbc.queryForObjets() can only provide primitive wrappers and String.")
            );
        }

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null && params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new NotFoundException("Data in query for object was not found."));
                }

                T value = WrappersMapper.map(resultSet, type);
                return Result.success(value);
            }
        } catch (SQLException | IllegalArgumentException e) {
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
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
     * Result<List<UserAccount>, Throwable> users = jdbc.readListOf(
     *          "SELECT * FROM UserAccount",
     *          this::userAccountMapper
     * );
     * </pre>
     */
    public <T> Result<List<T>, Throwable> readListOf(final String sql, final ResultSetExtractor<T> extractor,
                                                     final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql,
                     ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            if (params != null && params.length > 0) {
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
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return handleSQLException(e);
        }
    }

    public <T> Result<List<T>, Throwable> readListOf(final String sql, final ResultSetExtractor<T> extractor,
                                                     final ResultSetType resultSetType, final @Nullable Object... params) {

        if (sql == null) return Result.failure(new IllegalArgumentException("SQL query cannot be null"));
        if (extractor == null) return Result.failure(new IllegalArgumentException("Extractor cannot be null"));

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, resultSetType.type(), resultSetType.concurrency())) {
            if (params != null && params.length > 0) {
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
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
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
     * Result<Boolean, Throwable> result = jdbc.write(insertProductSQL, productName, productPrice);
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

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, args);
            statement.executeUpdate();

            return Result.success(Boolean.TRUE);
        } catch (SQLException e) {
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
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
     * Result<Boolean, Throwable> result = jdbc.writeArrayOf(
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

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, args);
            final Array createdArray = connection.createArrayOf(arrayDefinition, array);
            statement.setArray(arrayIndex, createdArray);
            statement.executeUpdate();

            return Result.success(true);
        } catch (SQLException e) {
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
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
     * Result<Boolean, Throwable> result = jdbc.writeBatch(insertCustomerSQL, batchArgs);
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

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Object[] args : batchArgs) {
                setParameters(statement, args);
                statement.addBatch();
            }

            statement.executeBatch();

            return Result.success(true);
        } catch (SQLException e) {
            Log.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
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
        for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
    }
}
