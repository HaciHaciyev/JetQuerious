package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql_error_translation.RepositoryException;
import io.github.hacihaciyev.types.TypeInlineException;
import io.github.hacihaciyev.types.TypeInstantiationException;
import io.github.hacihaciyev.types.internal.Field;
import io.github.hacihaciyev.types.internal.MetaRegistry;
import io.github.hacihaciyev.types.internal.RecordFactory;
import io.github.hacihaciyev.types.internal.TypeInfo;
import io.github.hacihaciyev.types.internal.TypeInfoOk;
import io.github.hacihaciyev.types.internal.TypeMeta;
import io.github.hacihaciyev.types.internal.TypeRegistry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class MapperBuilder<T> {

    private final Class<T> type;
    private final List<FieldDecl> decls;

    MapperBuilder(Class<T> type) {
        this(type, List.of());
    }

    private MapperBuilder(Class<T> type, List<FieldDecl> decls) {
        this.type  = requireNonNull(type, "type cannot be null");
        this.decls = List.copyOf(decls);
    }

    public MapperBuilder<T> field(String column, Class<?> type) {
        return appended(new FieldDecl.Plain(requireNonNull(column), requireNonNull(type)));
    }

    public MapperBuilder<T> valueObject(String column, Class<?> type) {
        return appended(new FieldDecl.ValueObject(requireNonNull(column), requireNonNull(type)));
    }

    public MapperBuilder<T> namingConventions(String column, Class<?> type) {
        return appended(new FieldDecl.Nested(requireNonNull(column), requireNonNull(type)));
    }

    public ResultSetExtractor<T> build(JQ jq) {
        requireNonNull(jq, "jq cannot be null");

        var record  = recordOf(type);
        var context = (Context) switch (jq) {
            case JQ.Read read   -> read.context();
            case JQ.Write write -> write.context();
        };
        var byName  = projectionByName(context.projectionColumns());

        decls.forEach(decl -> validate(decl, byName));
        requireFieldCount(record);

        return rs -> map(rs, record.factory());
    }

    static <T> MapperBuilder<T> declare(Class<T> type) {
        return new MapperBuilder<>(type);
    }

    static <T> MapperBuilder<T> namingConventions(Class<T> type) {
        var builder = new MapperBuilder<>(type);
        for (var field : recordOf(type).fields()) builder = builder.appended(declFor(field));
        return builder;
    }

    private static FieldDecl declFor(Field<?, ?> field) {
        return switch (TypeRegistry.info(field.type())) {
            case TypeInfo.Some _           -> new FieldDecl.Plain(field.name(), field.type());
            case TypeInfo.WithFactory<?> _ -> new FieldDecl.ValueObject(field.name(), field.type());
            case TypeInfo.None _           -> throw unsupportedNamingField(field);
        };
    }

    private static IllegalArgumentException unsupportedNamingField(Field<?, ?> field) {
        return new IllegalArgumentException(
            "Field '" + field.name() + "' has unsupported type " + field.type().getName() +
            " for namingConventions() — use declare() with explicit nested mapping instead"
        );
    }

    private void validate(FieldDecl decl, Map<String, ColumnRef> byName) {
        if (decl instanceof FieldDecl.Nested n) {
            validateNested(n, byName);
            return;
        }

        var cref = byName.get(decl.column());
        if (cref == null) throw new IllegalArgumentException("Column '" + decl.column() + "' not found in query projection");

        if (cref.typeClass() instanceof ColumnRef.Type.Some(var actual) && !actual.equals(decl.type()))
            throw new IllegalArgumentException(
                "Type mismatch for column '" + decl.column() + "': projection has " +
                actual.getName() + " but mapper expects " + decl.type().getName()
            );

        switch (decl) {
            case FieldDecl.Plain p        -> requireSupported(p.type(), TypeInfo.Some.class);
            case FieldDecl.ValueObject vo -> requireSupported(vo.type(), TypeInfo.WithFactory.class);
            case FieldDecl.Nested _       -> throw new IllegalStateException("unreachable");
        }
    }

    private void validateNested(FieldDecl.Nested n, Map<String, ColumnRef> byName) {
        requireNestable(n.type());

        for (var field : recordOf(n.type()).fields()) {
            var cref = byName.get(field.name());
            if (cref == null)
                throw new IllegalArgumentException(
                    "Column '" + field.name() + "' (required by nested type " + n.type().getName() +
                    ") not found in query projection"
                );

            if (cref.typeClass() instanceof ColumnRef.Type.Some(var actual) && !actual.equals(field.type()))
                throw new IllegalArgumentException(
                    "Type mismatch for column '" + field.name() + "': projection has " +
                    actual.getName() + " but nested type " + n.type().getName() + " expects " + field.type().getName()
                );
        }
    }

    private static void requireSupported(Class<?> type, Class<? extends TypeInfo> expectedKind) {
        if (!expectedKind.isInstance(TypeRegistry.info(type)))
            throw new IllegalArgumentException(
                "Type " + type.getName() + " is not supported as a " + describeKind(expectedKind)
            );
    }

    private static String describeKind(Class<? extends TypeInfo> kind) {
        return kind == TypeInfo.Some.class ? "plain field" : "value object";
    }

    private static void requireNestable(Class<?> type) {
        for (var field : recordOf(type).fields()) {
            if (TypeRegistry.info(field.type()) instanceof TypeInfo.None)
                throw new IllegalArgumentException(
                    "Field '" + field.name() + "' of " + type.getName() +
                    " is not a primitive or value object supported by namingConventions()"
                );
        }
    }

    private void requireFieldCount(TypeMeta.Record<T> record) {
        if (decls.size() != record.fields().length)
            throw new IllegalArgumentException(
                "Expected " + record.fields().length + " field declarations for " + type.getName() +
                " but got " + decls.size()
            );
    }

    private static Map<String, ColumnRef> projectionByName(List<ColumnRef> crefs) {
        var map = new HashMap<String, ColumnRef>();
        for (var cref : crefs) map.put(effectiveName(cref), cref);
        return map;
    }

    private static String effectiveName(ColumnRef cref) {
        return cref instanceof ColumnRef.AliasedColumn ac ? ac.alias() : cref.name();
    }

    @SuppressWarnings("unchecked")
    private static <R> TypeMeta.Record<R> recordOf(Class<R> type) {
        if (!(MetaRegistry.meta(type) instanceof TypeMeta.Record<?> rec)) throw new IllegalArgumentException(type.getName() + " is not a recognized record type");
        return (TypeMeta.Record<R>) rec;
    }

    private T map(ResultSet rs, RecordFactory<T> factory) throws SQLException, RepositoryException {
        try {
            var args = new Object[decls.size()];
            for (int i = 0; i < decls.size(); i++)
                args[i] = read(decls.get(i), rs);
            return factory.create(args);
        } catch (SQLException e) {
            throw e;
        } catch (TypeInstantiationException | TypeInlineException e) {
            throw new RepositoryException("HY000", "Failed to map row to " + type.getName() + ": " + e.getMessage(), e);
        }
    }

    private static Object read(FieldDecl decl, ResultSet rs) throws SQLException, TypeInlineException, TypeInstantiationException {
        return switch (decl) {
            case FieldDecl.Plain p        -> readColumn(rs, p.column(), p.type());
            case FieldDecl.ValueObject vo -> readColumn(rs, vo.column(), vo.type());
            case FieldDecl.Nested n       -> readNested(rs, n.type());
        };
    }

    private static Object readColumn(ResultSet rs, String column, Class<?> type) throws SQLException, TypeInlineException {
        if (!(TypeRegistry.info(type) instanceof TypeInfoOk ok)) throw new IllegalStateException("Unsupported type at runtime: " + type.getName());
        return ok.getter().get(rs, column);
    }

    private static <N> N readNested(ResultSet rs, Class<N> type) throws SQLException, TypeInlineException, TypeInstantiationException {
        var record = recordOf(type);
        var fields = record.fields();
        var args   = new Object[fields.length];

        for (int i = 0; i < fields.length; i++) args[i] = readColumn(rs, fields[i].name(), fields[i].type());

        return record.factory().create(args);
    }

    private MapperBuilder<T> appended(FieldDecl decl) {
        var next = new ArrayList<>(decls);
        next.add(decl);
        return new MapperBuilder<>(type, next);
    }
}