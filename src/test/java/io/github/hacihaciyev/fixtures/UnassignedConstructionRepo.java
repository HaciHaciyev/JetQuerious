package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class UnassignedConstructionRepo {

    public static final JQ.Write INSERT = insertInto("users")
        .columns("id", Long.class)
        .build();

    static {
        deleteFrom("users").where(eq(col("id"), param(Long.class))).build();
    }
}