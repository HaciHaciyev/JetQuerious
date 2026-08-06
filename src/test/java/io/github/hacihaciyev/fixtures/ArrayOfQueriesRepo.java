package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class ArrayOfQueriesRepo {

    public static final JQ.Read[] QUERIES = {
        select(col("id")).from("users").build(),
        select(col("name")).from("users").build()
    };
}