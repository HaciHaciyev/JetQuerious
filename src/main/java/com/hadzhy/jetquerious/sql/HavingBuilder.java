package com.hadzhy.jetquerious.sql;

public class HavingBuilder {
    private final StringBuilder query;

    HavingBuilder(StringBuilder query) {
        this.query = query;
    }

    public OrderByBuilder orderBy(String customOrder) {
        query.append("ORDER BY ").append(customOrder).append(" ");
        return new OrderByBuilder(query);
    }

    public SQLState limitAndOffset() {
        query.append("LIMIT ").append("? ").append("OFFSET ").append("? ");
        return new SQLState(this.query.toString());
    }

    public SQLState limitAndOffset(int limit, int offset) {
        query.append("LIMIT ").append(limit).append(" ").append("OFFSET ").append(offset).append(" ");
        return new SQLState(this.query.toString());
    }

    public SQLState build() {
        return new SQLState(this.query.toString());
    }
}
