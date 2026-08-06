package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class TooManyParamsCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void tooManyParams() {
        jq.write(CorrectRepo.DELETE, 1L, "extra", "another");
    }
}