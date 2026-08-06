package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class NullParamCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void insertWithNull() {
        jq.write(CorrectRepo.INSERT, null, "Alice");
    }
}