package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class AliasedFieldsRepo {

    public static final JQ.Read SELECT = select(col("id")).from("users").build();

    public static final JQ.Read SELECT_ALIAS = SELECT;
}