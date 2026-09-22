package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class CriteriaCorrectCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void byEmailAndAge_nullEmail() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id"))
            .args(null, 40)
            .many();
    }

    public void byEmailAndAge_bothNull() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id"))
            .args(null, (Object) null)
            .many();
    }

    public void byEmailAndAge_bothPresent() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id"))
            .args("a@example.com", 40)
            .one();
    }

    public void byActiveAndOptionalEmail_requiredPresent() {
        jq.criteria(CriteriaRepo.BY_ACTIVE_AND_OPTIONAL_EMAIL, rs -> rs.getLong("id"))
            .args(true, null)
            .one();
    }

    public void groupedByOptionalDepartment_null() {
        jq.criteria(CriteriaRepo.GROUPED_BY_OPTIONAL_DEPARTMENT, rs -> rs.getLong("count"))
            .args((Object) null)
            .many();
    }

    public void groupedByOptionalDepartment_value() {
        jq.criteria(CriteriaRepo.GROUPED_BY_OPTIONAL_DEPARTMENT, rs -> rs.getLong("count"))
            .args("unassigned")
            .many();
    }

    public void allClauses_allPresent() {
        jq.criteria(CriteriaRepo.ALL_CLAUSES_OPTIONAL, rs -> rs.getLong("count"))
            .args("name", true, 1L, false)
            .many();
    }

    public void allClauses_allNull() {
        jq.criteria(CriteriaRepo.ALL_CLAUSES_OPTIONAL, rs -> rs.getLong("count"))
            .args(null, null, null, null)
            .many();
    }

    public void noParams_withoutArgs() {
        jq.criteria(CriteriaRepo.NO_PARAMS, rs -> rs.getString("name")).many();
    }

    public void withResultSetType_leadingNull() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id"))
            .resultSetType(io.github.hacihaciyev.jdbc.ResultSetType.SCROLL_INSENSITIVE_READ_ONLY)
            .args(null, 40)
            .many();
    }
}