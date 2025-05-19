package com.hadzhy.jetquerious.config;

import com.hadzhy.jetquerious.jdbc.JetQuerious;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DBConfig {

    public DBConfig() {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl("jdbc:postgresql://localhost:30010/jetquerious");
        pgDataSource.setUser("root");
        pgDataSource.setPassword("password");

        JetQuerious.init(pgDataSource);
    }
}
