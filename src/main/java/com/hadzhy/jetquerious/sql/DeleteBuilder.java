package com.hadzhy.jetquerious.sql;

public class DeleteBuilder {
    final StringBuilder query;

    DeleteBuilder() {
        this.query = new StringBuilder("DELETE FROM ");
    }

    public DeleteFromBuilder from(String table) {
        return new DeleteFromBuilder(query.append(table).append(" "));
    }
}
