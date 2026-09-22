package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.QueryForge;
import io.github.hacihaciyev.sql.Criteria;

import static io.github.hacihaciyev.sql.SQL.*;

public class CriteriaRepo {

    public static final Criteria BY_EMAIL_AND_AGE = QueryForge.criteria(col("id"), col("name"), col("email"))
        .from("users")
        .where(and(
            eq(col("email"), opt(String.class)),
            eq(col("age"), opt(Integer.class))
        ))
        .build();

    public static final Criteria BY_ACTIVE_AND_OPTIONAL_EMAIL = QueryForge.criteria(col("id"), col("name"))
        .from("users")
        .where(and(
            eq(col("active"), param(Boolean.class)),
            eq(col("email"), opt(String.class))
        ))
        .build();

    public static final Criteria GROUPED_BY_OPTIONAL_DEPARTMENT = QueryForge.criteria(countAll())
        .from("employees")
        .groupBy(coalesce(col("department"), opt(String.class)))
        .build();

    public static final Criteria ALL_CLAUSES_OPTIONAL = QueryForge.criteria(col("active"), countAll())
        .from("users")
        .where(eq(col("name"), opt(String.class)))
        .groupBy(coalesce(col("active"), opt(Boolean.class)))
        .having(gt(countAll(), opt(Long.class)))
        .orderBy(coalesce(col("active"), opt(Boolean.class)))
        .build();

    public static final Criteria NO_PARAMS = QueryForge.criteria(col("name"))
        .from("users")
        .build();
}