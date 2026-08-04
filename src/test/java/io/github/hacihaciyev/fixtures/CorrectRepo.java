package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class CorrectRepo {

    public static final JQ.Write INSERT = insertInto("users")
        .columns("id", Long.class, "name", String.class)
        .build();

    public static final JQ.Read SELECT = select(col("id"), col("name"))
        .from("users")
        .where(eq(col("id"), param(Long.class)))
        .build();

    public static final JQ.Write DELETE = deleteFrom("users")
        .where(eq(col("id"), param(Long.class)))
        .build();
}