package com.hadzhy.jetquerious.jdbc;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;

class JetQueriousTest {

    PostgreSQLContainer<?> pgvector = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @Test
    void start() {
        pgvector.start();

        try {
            pgvector.createConnection("");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
