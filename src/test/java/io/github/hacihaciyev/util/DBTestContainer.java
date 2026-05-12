package io.github.hacihaciyev.util;

import io.github.hacihaciyev.config.Conf;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

public class DBTestContainer implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        initialize();
    }

    public static void initialize() throws Exception {
        if (POSTGRES.isRunning()) return;

        POSTGRES.start();

        var ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());

        Conf.INSTANCE.defDataSource(ds);

        try (var conn = ds.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id      BIGINT,
                    name    VARCHAR,
                    email   VARCHAR,
                    active  BOOLEAN,
                    age     INTEGER
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id      BIGINT,
                    user_id BIGINT,
                    total   NUMERIC,
                    amount  NUMERIC,
                    status  VARCHAR
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id       BIGINT,
                    order_id BIGINT,
                    product  VARCHAR,
                    qty      INTEGER
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS employees (
                    id         BIGINT,
                    department VARCHAR,
                    salary     NUMERIC,
                    active     BOOLEAN
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS banned_users (
                    id      BIGINT,
                    user_id BIGINT
                )
            """);
        }
    }

    @Override
    public void close() {
        if (POSTGRES.isRunning()) POSTGRES.stop();
    }
}