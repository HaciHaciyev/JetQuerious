package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class CriteriaWrongArgCountCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void tooFewArgs() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id"))
            .args((Object) null)
            .many();
    }

    public void tooManyArgs() {
        jq.criteria(CriteriaRepo.BY_ACTIVE_AND_OPTIONAL_EMAIL, rs -> rs.getLong("id"))
            .args(true, "extra@example.com", "another")
            .many();
    }

    public void noArgsForParameterizedQuery() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id")).many();
    }

    public void argsForParameterlessQuery() {
        jq.criteria(CriteriaRepo.NO_PARAMS, rs -> rs.getString("name"))
            .args("unexpected")
            .many();
    }

    public void tooFewArgsAcrossClauses() {
        jq.criteria(CriteriaRepo.ALL_CLAUSES_OPTIONAL, rs -> rs.getLong("count"))
            .args("name", true)
            .many();
    }
}