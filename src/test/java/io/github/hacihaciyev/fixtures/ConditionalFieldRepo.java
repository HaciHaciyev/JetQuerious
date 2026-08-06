package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class ConditionalFieldRepo {

    public static final JQ.Read SELECT = System.currentTimeMillis() > 0
        ? select(col("id")).from("users").build()
        : select(col("name")).from("users").build();
}