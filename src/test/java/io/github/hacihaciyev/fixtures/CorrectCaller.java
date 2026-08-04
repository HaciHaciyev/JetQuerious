package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class CorrectCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void insert() { jq.write(CorrectRepo.INSERT, 1L, "Alice"); }
    public void select() { jq.one(CorrectRepo.SELECT, rs -> rs.getLong("id"), 1L); }
    public void delete() { jq.write(CorrectRepo.DELETE, 1L); }
}