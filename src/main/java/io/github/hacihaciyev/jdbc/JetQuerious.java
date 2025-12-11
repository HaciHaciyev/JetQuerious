package io.github.hacihaciyev.jdbc;

public class JetQuerious {
    /*
    private DataSource dataSource;
    private JetQExecutor executor;
    private static JetQuerious instance;
    private static final Logger LOG = Logger.getLogger(JetQuerious.class.getName());

    private JetQuerious(DataSource dataSource, JetQExecutor executor) {
        if (dataSource == null)
            throw new IllegalArgumentException("Provided datasource is null");

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
            throw new IllegalStateException("JetQuerious is not initialized. Call JetQuerious.init(dataSource) first.");
        return instance;
    }

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

    public Result<Void, Exception> transactional(TransactionContext<Connection> action) {
        if (action == null)
            return new Err<>(new IllegalArgumentException("Action must not be null"));

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            return commitTransaction(action, connection);
        } catch (Exception t) {
            LOG.log(Level.SEVERE, "Exception during transaction", t);
            return new Err<>(t);
        }
    }

    private static Result<Void, Exception> commitTransaction(TransactionContext<Connection> action,
                                                             Connection connection) throws SQLException {
        try {
            action.accept(connection);
            connection.commit();
            return new Ok<>(null);
        } catch (Exception t) {
            connection.rollback();
            return new Err<>(t);
        }
    }

    public void stepInTransaction(final Connection connection, final String sql,
            final @Nullable Object... params) throws TransactionException {

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

    public <T> Result<T, Exception> execute(
            String sql,
            SQLFunction<PreparedStatement, T> callback,
            @Nullable Object... params) {

        try {
            return new Ok<>(doExecute(sql, callback, params));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> handleSQLException(sqlException);
                default -> new Err<>(e);
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
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private <T> T doExecute(
            String sql,
            SQLFunction<PreparedStatement, T> callback,
            @Nullable Object... params) throws Exception {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (callback == null)
            throw new IllegalArgumentException("Callback function cannot be null");

        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, params);
            return callback.apply(statement);
        }
    }

    public <T> Result<T, Exception> read(final String sql, final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        try {
            return new Ok<>(doRead(sql, extractor, params));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error: %s".formatted(e.getMessage()));
            return switch (e) {
                case SQLException sqlException -> handleSQLException(sqlException);
                default -> new Err<>(e);
            };
        }
    }

    public <T> Result<T, Exception> read(final String sql, final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) {

        try {
            return new Ok<>(doRead(sql, extractor, resultSetType, params));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> handleSQLException(sqlException);
                default -> new Err<>(e);
            };
        }
    }

    public <T> CompletionStage<T> asynchRead(final String sql,
            final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doRead(sql, extractor, params);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    public <T> CompletionStage<T> asynchRead(final String sql,
            final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doRead(sql, extractor, resultSetType, params);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private  <T> T doRead(final String sql, final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) throws Exception {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (extractor == null)
            throw new IllegalArgumentException("Extractor cannot be null");
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

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

    private  <T> T doRead(final String sql, final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) throws Exception {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (extractor == null)
            throw new IllegalArgumentException("Extractor cannot be null");
        if (resultSetType == null)
            throw new IllegalArgumentException("Result set type can`t be null");
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

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

    public <T> Result<T, Exception> readObjectOf(final String sql, final Class<T> type,
            final @Nullable Object... params) {

        try {
            return new Ok<>(doReadObjectOf(sql, type, params));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> new Err<>(((Err<?, ?>) handleSQLException(sqlException)).err());
                default -> new Err<>(e);
            };
        }
    }

    public <T> CompletionStage<T> asynchReadObjectOf(final String sql, final Class<T> type,
            final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doReadObjectOf(sql, type, params);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private  <T> T doReadObjectOf(final String sql, final Class<T> type,
                                final @Nullable Object... params) throws Exception {

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
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Data in query for object was not found.");
                }

                T value = null; // TODO OutboundMapper.map(resultSet, type);
                return value;
            }
        }
    }

    public <T> Result<List<T>, Exception> readListOf(final String sql, final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        try {
          return new Ok<>(doReadListOf(sql, extractor, params));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> new Err<>(((Err<?, ?>) handleSQLException(sqlException)).err());
                default -> new Err<>(e);
            };
        }
    }

    public <T> Result<List<T>, Exception> readListOf(final String sql, final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType, final @Nullable Object... params) {

        try {
          return new Ok<>(doReadListOf(sql, extractor, resultSetType, params));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                default -> sneakyThrow(e);
            };
        }
    }

    public <T> CompletionStage<List<T>> asynchReadListOf(final String sql,
            final ResultSetExtractor<T> extractor,
            final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doReadListOf(sql, extractor, params);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    public <T> CompletionStage<List<T>> asynchReadListOf(final String sql,
            final ResultSetExtractor<T> extractor,
            final ResultSetType resultSetType,
            final @Nullable Object... params) {

        return executor.execute(() -> {
            try {
                return doReadListOf(sql, extractor, resultSetType, params);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private <T> List<T> doReadListOf(final String sql, final ResultSetExtractor<T> extractor,
                                     final @Nullable Object... params) throws Exception {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (extractor == null)
            throw new IllegalArgumentException("Extractor cannot be null");
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

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

            return results;
        }
    }

    private <T> List<T> doReadListOf(final String sql, final ResultSetExtractor<T> extractor,
                                     final ResultSetType resultSetType, final @Nullable Object... params) throws Exception {

        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (extractor == null)
            throw new IllegalArgumentException("Extractor cannot be null");
        if (resultSetType == null)
            throw new IllegalArgumentException("Result set type can`t be null");
        var typesResult = validateArgumentsTypes(params);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

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

            return results;
        }
    }

    public Result<Integer, Exception> write(final String sql, final Object... args) {
        try {
            return new Ok<>(doWrite(sql, args));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> new Err<>(((Err<?, ?>) handleSQLException(sqlException)).err());
                default -> new Err<>(e);
            };
        }
    }

    public CompletionStage<Integer> asynchWrite(final String sql, final Object... args) {
        return executor.execute(() -> {
            try {
                return doWrite(sql, args);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private Integer doWrite(String sql, Object[] args) throws Exception {
        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (args == null)
            throw new IllegalArgumentException("Arguments cannot be null");
        var typesResult = validateArgumentsTypes(args);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setParameters(statement, args);
                int affectedRows = statement.executeUpdate();

                connection.commit();
                return affectedRows;
            }
        } catch (SQLException e) {
            rollback(connection);
            throw ((Err<?, ?>) handleSQLException(e)).err();
        } finally {
            close(connection);
        }
    }

    public Result<Integer, Exception> writeArrayOf(final String sql, final String arrayDefinition,
            final byte arrayIndex,
            final Object[] array, final Object... args) {

        try {
            return new Ok<>(doWriteArray(sql, arrayDefinition, arrayIndex, array, args));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> new Err<>(((Err<?, ?>) handleSQLException(sqlException)).err());
                default -> new Err<>(e);
            };
        }
    }

    public CompletableFuture<Integer> asynchWriteArrayOf(final String sql,
            final String arrayDefinition,
            final byte arrayIndex, final Object[] array,
            final Object... args) {

        return executor.execute(() -> {
            try {
                return doWriteArray(sql, arrayDefinition, arrayIndex, array, args);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private Integer doWriteArray(String sql, String arrayDefinition, byte arrayIndex, Object[] array, Object[] args) throws Exception {
        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (arrayDefinition == null)
            throw new IllegalArgumentException("Array definition cannot be null");
        if (arrayIndex < 1)
            throw new IllegalArgumentException("Array index can`t be below 1");
        if (array == null)
            throw new IllegalArgumentException("Array cannot be null");
        if (args == null)
            throw new IllegalArgumentException("Arguments cannot be null");

        var typesResult = validateArgumentsTypes(args);
        if (!typesResult.isOk())
            throw ((Err<?, ?>) typesResult).err();

        var arrayRes = validateArray(array, arrayDefinition);
        if (!arrayRes.isOk())
            throw ((Err<?, ?>) arrayRes).err();

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
                return affectedRows;
            }
        } catch (SQLException e) {
            rollback(connection);
            throw ((Err<?, ?>) handleSQLException(e)).err();
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

    public Result<int[], Exception> writeBatch(final String sql, final List<Object[]> batchArgs) {
        try {
            return new Ok<>(doWriteBatch(sql, batchArgs));
        } catch (Exception e) {
            return switch (e) {
                case SQLException sqlException -> new Err<>(((Err<?, ?>) handleSQLException(sqlException)).err());
                default -> new Err<>(e);
            };
        }
    }

    public CompletableFuture<int[]> asynchWriteBatch(final String sql,
            final List<Object[]> batchArgs) {

        return  executor.execute(() -> {
            try {
                return doWriteBatch(sql, batchArgs);
            } catch (Exception e) {
                return switch (e) {
                    case SQLException sqlException -> sneakyThrow(((Err<?, ?>) handleSQLException(sqlException)).err());
                    default -> sneakyThrow(e);
                };
            }
        });
    }

    private int[] doWriteBatch(String sql, List<Object[]> batchArgs) throws Exception {
        if (sql == null)
            throw new IllegalArgumentException("SQL query cannot be null");
        if (batchArgs == null)
            throw new IllegalArgumentException("Batch arguments cannot be null");
        for (Object[] params : batchArgs) {
            var typesResult = validateArgumentsTypes(params);
            if (!typesResult.isOk())
                throw ((Err<?, ?>) typesResult).err();
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
                return affectedCounts;
            }
        } catch (SQLException e) {
            rollback(connection);
            throw ((Err<?, ?>) handleSQLException(e)).err();
        } finally {
            close(connection);
        }
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
    private static <E extends Exception, R> R sneakyThrow(Exception e) throws E {
        throw (E) e;
    }

    private void setParameters(final PreparedStatement statement, final Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            //ParameterSetter.setParameter(statement, param, i + 1); TODO
        }
    }

    private Result<Void, Exception> validateArray(Object[] array, String arrayDefinition) {
        try {
            // TODO LegacyTypeRegistry.validateArrayElementsMatchDefinition(array, ArrayDefinition.from(arrayDefinition));
            return new Ok<>(null);
        } catch (IllegalArgumentException e) {
            return new Err<>(new InvalidArgumentTypeException(e.getMessage()));
        }
    }

    private Result<Void, InvalidArgumentTypeException> validateArgumentsTypes(final @Nullable Object... params) {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
             TODO if (!LegacyTypeRegistry.isSupportedType(param)) {
                String className = param.getClass().getName();
                String simpleName = param.getClass().getSimpleName();
                String packageName = param.getClass().getPackage() != null ? param.getClass().getPackage().getName()
                        : "unknown package";

                return new Err<(new InvalidArgumentTypeException(
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

        return new Ok<>(null);
    }
    */
}
