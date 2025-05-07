package com.hadzhy.jetquerious.sql;

public class OrderByBuilder {
    private final StringBuilder query;

    OrderByBuilder(StringBuilder query) {
        this.query = query;
    }

    public SQLState limitAndOffset(int limit, int offset) {
        query.append("LIMIT ").append(limit).append(" ").append("OFFSET ").append(offset).append(" ");
        return new SQLState(this.query.toString());
    }

    public SQLState limitAndOffset() {
        query.append("LIMIT ").append("? ").append("OFFSET ").append("? ");
        return new SQLState(this.query.toString());
    }

    public SQLState build() {
        return new SQLState(this.query.toString());
    }
}
