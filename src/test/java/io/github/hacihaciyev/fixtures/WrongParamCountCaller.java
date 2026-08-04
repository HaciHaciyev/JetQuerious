package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.jdbc.ResultSetExtractor;

public class WrongParamCountCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void tooFewParams() {
        jq.write(CorrectRepo.INSERT, 1L);
    }

    public void tooManyParams() {
        jq.write(CorrectRepo.DELETE, 1L, "extra", "another");
    }
}