package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;

public class CriteriaWrongTypeCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void wrongTypeForOptionalSlot() {
        jq.criteria(CriteriaRepo.BY_EMAIL_AND_AGE, rs -> rs.getLong("id"))
            .args(40, 40)
            .many();
    }

    public void wrongTypeForRequiredSlot() {
        jq.criteria(CriteriaRepo.BY_ACTIVE_AND_OPTIONAL_EMAIL, rs -> rs.getLong("id"))
            .args("not-a-boolean", null)
            .many();
    }

    public void wrongTypeForGroupBySlot() {
        jq.criteria(CriteriaRepo.GROUPED_BY_OPTIONAL_DEPARTMENT, rs -> rs.getLong("count"))
            .args(123)
            .many();
    }

    public void wrongTypeForHavingSlotAmongFour() {
        jq.criteria(CriteriaRepo.ALL_CLAUSES_OPTIONAL, rs -> rs.getLong("count"))
            .args("name", true, "not-a-long", false)
            .many();
    }
}