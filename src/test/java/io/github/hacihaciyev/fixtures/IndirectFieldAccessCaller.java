package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.sql.JQ;

public class IndirectFieldAccessCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    private static JQ.Write insertQuery() {
        return CorrectRepo.INSERT;
    }

    public void insert() {
        jq.write(insertQuery(), 1L, "Alice");
    }
}