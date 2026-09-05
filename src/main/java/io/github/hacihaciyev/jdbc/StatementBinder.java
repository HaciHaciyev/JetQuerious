package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.value_objects.ParamType;
import io.github.hacihaciyev.types.TypeInlineException;
import io.github.hacihaciyev.types.internal.TypeInfo;
import io.github.hacihaciyev.types.internal.TypeRegistry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static scala.jdk.javaapi.CollectionConverters.asJava;

final class StatementBinder {

    private StatementBinder() {}

    static void bind(PreparedStatement stmt, Context ctx, Object[] args) throws SQLException {
        var types = asJava(ctx.paramTypes());

        if (types.isEmpty()) {
            if (args.length != 0) {
                var actualCount = actualPlaceholderCount(stmt, 0);
                throw new IllegalArgumentException(
                    "Parameter count mismatch: statement has " + actualCount + " placeholder(s), "
                        + "0 declared parameter type(s), and " + args.length + " argument(s) were provided."
                );
            }
            return;
        }

        if (args.length != types.size()) {
            throw new IllegalArgumentException(
                "Parameter count mismatch: " + types.size() + " declared parameter type(s) "
                    + "but " + args.length + " argument(s) were provided."
            );
        }

        for (int i = 0; i < types.size(); i++) {
            var paramType = types.get(i);
            var arg       = args[i];
            var typeInfo  = TypeRegistry.info(paramType._type());

            if (!(typeInfo instanceof TypeInfo.Some some)) {
                throw new IllegalArgumentException("Unsupported type: " + paramType._type().getName() + " at position " + (i + 1));
            }

            try {
                some.setter().set(stmt, arg, paramType.position());
            } catch (TypeInlineException e) {
                throw new SQLException("Failed to bind parameter at position " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }

    private static int actualPlaceholderCount(PreparedStatement stmt, int fallback) {
        try {
            return stmt.getParameterMetaData().getParameterCount();
        } catch (SQLException e) {
            return fallback;
        }
    }

    static List<ParamType> paramTypes(JQ jq) {
        return asJava(switch (jq) {
            case JQ.Read(_, var ctx)  -> ((Context) ctx).paramTypes();
            case JQ.Write(_, var ctx) -> ((Context) ctx).paramTypes();
        });
    }
}