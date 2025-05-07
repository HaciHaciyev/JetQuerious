package com.hadzhy.jdbclight.jdbc;

import java.sql.ResultSet;

public enum ResultSetType {
    FORWARD_ONLY_READ_ONLY(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY),
    FORWARD_ONLY_UPDATABLE(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE),
    SCROLL_INSENSITIVE_READ_ONLY(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY),
    SCROLL_INSENSITIVE_UPDATABLE(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE),
    SCROLL_SENSITIVE_READ_ONLY(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY),
    SCROLL_SENSITIVE_UPDATABLE(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

    private final int type;
    private final int concurrency;

    ResultSetType(int type, int concurrency) {
        this.type = type;
        this.concurrency = concurrency;
    }

    public int type() {
        return type;
    }

    public int concurrency() {
        return concurrency;
    }
}

