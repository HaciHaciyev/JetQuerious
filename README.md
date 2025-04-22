```markdown
# JDBC Class API Documentation

The `JDBC` class provides a set of utility methods for interacting with a relational database using JDBC (Java Database Connectivity). It simplifies SQL query execution, result handling, error handling, and transaction management. This class is designed to be used as a singleton within a Dependency Injection (DI) container.

## Key Features

- **Read Operations:** Execute SQL queries to fetch single values, objects, or lists of objects.
- **Write Operations:** Execute SQL updates, including single updates, array-based updates, and batch operations.
- **Error Handling:** Built-in mechanisms for handling SQL exceptions and translating them into application-specific exceptions.
- **Transaction Management:** Automatic management of transactions for write operations, ensuring data integrity.

## Usage Guidelines

### 1. Initialization

Ensure the `JDBC` instance is properly initialized with a valid `DataSource` before use.

### 2. Read Operations

Use the `read()` method to fetch single values or objects. For complex mappings, use the `read()` method with a `ResultSetExtractor` or `RowMapper`.

### 3. Write Operations

- Use the `write()` method for standard updates.
- For updates involving arrays, use `writeArrayOf()`.
- For bulk updates, use `writeBatch()`.

### 4. Parameter Handling

Always sanitize and validate parameters passed to SQL statements to prevent SQL injection.

### 5. Error Handling
The library implements a robust mapping of the SQLException with detailed descriptions and status codes
Check the result of each operation using the `Result` object. 
Use it to determine success or failure and handle errors accordingly.

### 6. Connection Management

The class manages database connections internally, so no need to manually open or close connections. Ensure that the `DataSource` is correctly configured.

## Example Usage

### 1. Initialize the JDBC Instance

```java
// Initialize the JDBC instance with a DataSource
DataSource dataSource = ...; // Obtain a DataSource instance
JDBC.init(dataSource);
JDBC jdbc = JDBC.instance();
```

### 2. Insert

```java
String insertSQL = insert()
     .into("products")
     .column("name")
     .column("price")
     .values()
     .build()
     .sql();

Result<Boolean, Throwable> insertResult = jdbc.write(insertSQL, "New Product", 29.99);
```

### 3. Fetch

```java
String selectSQL = select()
     .all()
     .from("products")
     .where("id = ?")
     .build()
     .sql();

Result<Product, Throwable> productResult = jdbc.read(selectSQL, Product.class, 1);
```

### 4. Update array

```java
String updateTagsSQL = update("products")
     .set("tags = ?")
     .where("id = ?")
     .build()
     .sql();

String[] tags = {"electronics", "sale"};
Result<Boolean, Throwable> updateResult = jdbc.writeArrayOf(updateTagsSQL, "text", 1, tags, 1);
```

### 5. Batch

```java
String batchInsertSQL = insert()
            .into("customers")
            .columns("name", "email")
            .values()
            .build()
            .sql();

List<Object[]> batchArgs = Arrays.asList(
    new Object[]{"Alice", "alice@example.com"},
    new Object[]{"Bob", "bob@example.com"}
);

Result<Boolean, Throwable> batchResult = jdbc.writeBatch(batchInsertSQL, batchArgs);
```

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.