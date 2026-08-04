package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class MultiParamRepo {

    public static final JQ.Read SEARCH = select(col("id"), col("name"))
        .from("users")
        .where(and(
            eq(col("name"),   param(String.class)),
            eq(col("active"), param(Boolean.class))
        ))
        .build();

    public static final JQ.Write UPDATE_MANY = update("users")
        .set("name", String.class, "email", String.class)
        .where(eq(col("id"), param(Long.class)))
        .build();
}