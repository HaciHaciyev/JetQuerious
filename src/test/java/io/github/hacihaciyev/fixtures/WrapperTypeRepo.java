package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.types.AsObject;
import io.github.hacihaciyev.types.AsString;
import io.github.hacihaciyev.types.UUIDStrategy;

import static io.github.hacihaciyev.sql.QueryForge.*;
import static io.github.hacihaciyev.sql.SQL.*;

public class WrapperTypeRepo {

    public static final JQ.Write INSERT_WITH_UUID_NATIVE = insertInto("users")
        .columns("id", Long.class, "name", String.class, "email", UUIDStrategy.Native.class)
        .build();

    public static final JQ.Write UPDATE_UUID_NATIVE = update("users")
        .set("email", UUIDStrategy.Native.class)
        .where(eq(col("id"), param(Long.class)))
        .build();

    public static final JQ.Write UPDATE_UUID_CHARSEQ = update("users")
        .set("email", UUIDStrategy.Charseq.class)
        .where(eq(col("id"), param(Long.class)))
        .build();

    public static final JQ.Write UPDATE_UUID_BINARY = update("users")
        .set("email", UUIDStrategy.Binary.class)
        .where(eq(col("id"), param(Long.class)))
        .build();

    public static final JQ.Write UPDATE_AS_STRING = update("users")
        .set("name", AsString.class)
        .where(eq(col("id"), param(Long.class)))
        .build();

    public static final JQ.Write UPDATE_AS_OBJECT = update("users")
        .set("name", AsObject.class)
        .where(eq(col("id"), param(Long.class)))
        .build();
}