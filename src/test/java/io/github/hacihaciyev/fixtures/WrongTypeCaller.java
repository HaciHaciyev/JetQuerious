package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class WrongTypeCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void wrongTypeForId() {
        jq.write(CorrectRepo.INSERT, "notALong", "Alice");
    }
}