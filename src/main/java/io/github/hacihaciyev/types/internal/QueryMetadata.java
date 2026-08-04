package io.github.hacihaciyev.types.internal;

import scala.jdk.javaapi.CollectionConverters;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.value_objects.ParamType;

import java.util.ArrayList;
import java.util.List;

record QueryMetadata(List<ParamType> paramTypes) {

    static QueryMetadata from(Context context) {
        return new QueryMetadata(new ArrayList<>(CollectionConverters.asJava(context.paramTypes())));
    }
}