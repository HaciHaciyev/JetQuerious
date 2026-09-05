package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.value_objects.ParamType;
import io.github.hacihaciyev.types.TypeInlineException;
import io.github.hacihaciyev.types.internal.MetaRegistry;
import io.github.hacihaciyev.types.internal.TypeInfo;
import io.github.hacihaciyev.types.internal.TypeMeta;
import io.github.hacihaciyev.types.internal.TypeRegistry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static scala.jdk.javaapi.CollectionConverters.asJava;

final class StatementBinder {

    private StatementBinder() {}

    static void bind(PreparedStatement stmt, Context ctx, Object[] args) throws SQLException {
        var types    = asJava(ctx.paramTypes());
        var expanded = expand(args, types);

        if (types.isEmpty()) {
            if (expanded.length != 0) {
                var actualCount = actualPlaceholderCount(stmt, 0);
                throw new IllegalArgumentException(
                    "Parameter count mismatch: statement has " + actualCount + " placeholder(s), "
                        + "0 declared parameter type(s), and " + expanded.length + " argument(s) were provided."
                );
            }
            return;
        }

        if (expanded.length != types.size()) {
            throw new IllegalArgumentException(
                "Parameter count mismatch: " + types.size() + " declared parameter type(s) "
                    + "but " + expanded.length + " argument(s) were provided."
            );
        }

        for (int i = 0; i < types.size(); i++) {
            var paramType = types.get(i);
            var arg       = expanded[i];
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

    private static Object[] expand(Object[] args, List<ParamType> types) {
        var expanded = new ArrayList<Object>(args.length);
        var typeIdx  = 0;

        for (var arg : args) {
            if (arg instanceof Deconstruction dec) {
                typeIdx = expandOne(dec, types, typeIdx, expanded);
                continue;
            }

            expanded.add(arg);
            typeIdx++;
        }

        return expanded.toArray();
    }

    private static int expandOne(Deconstruction dec, List<ParamType> types, int typeIdx, List<Object> out) {
        var recordType = dec.record().getClass();

        if (!(MetaRegistry.meta(recordType) instanceof TypeMeta.Record<?> rec)) {
            throw new IllegalArgumentException("Deconstruction target " + recordType.getName() + " is not a recognized record type");
        }

        var fields = rec.fields();
        var count  = switch (dec.limit()) {
            case Deconstruction.DeconstructionLimit.All _            -> fields.length;
            case Deconstruction.DeconstructionLimit.Specified(var n) -> n;
        };

        if (typeIdx + count > types.size()) {
            throw new IllegalArgumentException(
                "Deconstruction of " + recordType.getSimpleName() + " needs " + count +
                " parameter(s) starting at position " + (typeIdx + 1) + " but only " +
                (types.size() - typeIdx) + " declared parameter(s) remain"
            );
        }

        for (var i = 0; i < count; i++) {
            var expected = types.get(typeIdx)._type();
            var actual   = fields[i].type();

            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(
                    "Deconstruction of " + recordType.getSimpleName() + " field '" + fields[i].name() +
                    "' has type " + actual.getName() + " but declared parameter " + (typeIdx + 1) +
                    " expects " + expected.getName()
                );
            }

            typeIdx++;
        }

        var values = dec.deconstruct();
        for (var i = 0; i < count; i++) out.add(values[i]);

        return typeIdx;
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