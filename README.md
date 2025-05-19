# JetQuerious – SQL that flies

JetQuerious is a minimalistic persistence library for Java that makes SQL safe, expressive, and clean — without abstracting it away. 
It’s not an ORM. It doesn’t generate queries for you. It doesn’t want to "own" your data layer.
Instead, JetQuerious gives you just enough tools to write plain SQL with confidence, while keeping full control.
It reduces boilerplate, provides safe parameter handling,
and integrates seamlessly with raw SQL – no ORM, no magic, just fast and flexible database access.

At the center of it all is the `JetQuerious` class — the main entry point and your primary companion for all database interactions. 
It wraps JDBC in a functional, result-oriented API that handles connections, transactions, 
mapping, and errors — but leaves your logic and SQL untouched.

---

## Philosophy

JetQuerious is built on the idea that most projects don’t need a heavy abstraction 
over SQL — they just need sane defaults and a few guardrails.

We don’t try to reinvent querying. We don't introduce a custom DSL. We don’t care about annotations or reflection.
We just want you to be able to:

* Write SQL the way you already know it.
* Handle results and errors cleanly.
* Stop repeating yourself with resource management.
* Compose DB logic in a predictable, testable way.

If you're looking for something to hide the database behind objects — you're in the wrong place. 
If you want to write logic directly against SQL, and have a lightweight toolkit that helps you do that safely, welcome to JetQuerious.

---

## Core API: `JetQuerious`

All core features live in a single class: JetQuerious.
This is your gateway to querying, updating, inserting, building SQL statements, and wrapping code in transactions. 
You get full control, without the noise.

In addition to basic read/write operations, JetQuerious integrates seamlessly 
with QueryForge — a fluent utility for dynamically constructing SQL queries in a safe and composable way.
It helps reduce manual string concatenation, minimizes the risk of SQL injection, 
and makes complex query construction easier to maintain.

Here’s what using it feels like:

### Initialize the JetQuerious instance with a DataSource

You have to initialize the instance once. Then you can have this instance everywhere.

```java
DataSource dataSource = ...; // Obtain a DataSource instance
JetQuerious.init(dataSource);
JetQuerious jetQuerious = JetQuerious.instance();
```

### Fetching Data

To load a user by email:

```java
Result<UserAccount, Throwable> userResult = jetQuerious.read(
        "SELECT * FROM user_account", 
        this::userAccountMapper,
        email
);

UserAccount userAccountMapper(ResultSet rs) throws SQLException {
    return new UserAccount(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("hashed_password"),
            rs.getTimestamp("created_at").toInstant()
    );
}
```

It’s that simple. No manual resource closing. No `try/catch` hell. Just `Result<T>` — success or failure, clearly separated.
Yoe need to write mapper only once

Fetching a list?

```java
import static com.hadzhy.jetquerious.sql.QueryForge.*;

String users = select()
        .all()
        .from("user_account")
        .build()
        .sql();

Result<List<UserAccount>, Throwable> result = jet.readListOf(users, this::userAccountMapper);

// or you can use asynch version

CompletableFuture<Result<List<UserAccount>, Throwable>> users = jet.asynchReadListOf(users, this::userAccountMapper);
```

No need to pass a parameter setter if the query doesn’t have placeholders. JetQuerious makes both cases smooth.

---

### Writing Data

To insert a user:

```java
String saveUser = insert()
        .into("user_account")
        .column("username")
        .column("email")
        .values()
        .build()
        .sql();

Result<Boolean, Throwable> result = jet.write(saveUser, username, email);

// or asynch version

CompletableFuture<Result<Boolean, Throwable>> asynchResult = jet.asynchWrite(saveUser, username, email);
```

---

### Transactions

Every method you call on JetQuerious is already transactional — each call is isolated, and auto-rolled back on failure.

But if you want to compose several steps inside a single transaction:

```java
jetQuerious.transactional(conn -> {
    jetQuerious.stepInTransaction(conn, "UPDATE accounts SET balance = balance - ? WHERE id = ?", 100, 1);
    jetQuerious.stepInTransaction(conn, "UPDATE accounts SET balance = balance + ? WHERE id = ?", 100, 2);
});
```

---

### Safe Results

Every operation in JetQuerious returns a `Result<T>`. No exceptions are thrown from your query methods — ever.
You always get one of two things:

* `Result.success(value)`
* `Result.failure(e)`

You handle both explicitly:

```java
if (result.isSuccess()) {
    User user = result.value();
} else {
    log.warn("Failed to load user: {}", result.error());
}

result.ifSuccess(count -> log.info(""));
result.ifFailure(e -> log.error(""));

User user = result.orElseGet(new User(...));

// Wrapping checked exception call
Result<String, Exception> emailResult = Result.ofThrowable(() ->
        jet.readObjectOf("SELECT email FROM user_account WHERE id = ?", String.class, id));
```

This means fewer surprises, no hidden failures, and complete control over your error handling strategy.

---

## Not a Framework

JetQuerious isn’t a framework. There’s no configuration. No magic context. No lifecycle. 
You create an instance (backed by a `DataSource`) and you’re done.

It works just as well in a plain Java app, a Quarkus microservice, a Spring-based backend, or even a desktop tool. 
The API is small and stable. The goal is long-term clarity, not short-term convenience.

---

## License

This project is licensed under the [MIT License](LICENSE).