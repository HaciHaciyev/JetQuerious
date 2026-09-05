package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;

import static io.github.hacihaciyev.sql.SQL.*;
import static io.github.hacihaciyev.sql.QueryForge.*;

public class DeconstructionRepo {

    public static final JQ.Write UPDATE_NAME_EMAIL = update("users")
        .set("name", String.class, "email", String.class)
        .where(eq(col("id"), param(Long.class)))
        .build();
}
