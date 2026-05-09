package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBJqwikHook;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.AddLifecycleHook;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@AddLifecycleHook(DBJqwikHook.class)
class ContextPropertyTest {

    private static final Set<String> USERS_COLUMNS        = Set.of("id", "name", "email", "active");
    private static final Set<String> ORDERS_COLUMNS       = Set.of("id", "user_id", "total", "status");
    private static final Set<String> ORDER_ITEMS_COLUMNS  = Set.of("id", "order_id", "product", "qty");
    private static final Set<String> ALL_COLUMNS          = Set.of("id", "name", "email", "active", "user_id", "total", "status", "order_id", "product", "qty");
    private static final Set<String> ALL_TABLES           = Set.of("users", "orders", "order_items");

    private static TableSource.Physical physical(String table) {
        return new TableSource.Physical(new TableRef.Base(table));
    }

    private static TableSource.Physical physical(String table, String alias) {
        return new TableSource.Physical(new TableRef.AliasedBase(table, alias));
    }

    private static Ref namedRef(String col) {
        return new Ref.Named(new Projection.Base(new ColumnRef.Base(col)));
    }

    private static Ref namedRef(String col, Class<?> type) {
        return new Ref.Named(new Projection.Base(new ColumnRef.Base(col, new ColumnRef.Type.Some(type))));
    }

    private static Ref aliasedRef(String table, String col, String alias) {
        return new Ref.Named(new Projection.Base(new ColumnRef.VariableAlias(table, col, alias)));
    }

    private boolean isKnownColumn(String name) {
        return ALL_COLUMNS.stream().anyMatch(c -> c.equalsIgnoreCase(name));
    }

    private boolean isKnownTable(String name) {
        return ALL_TABLES.stream().anyMatch(t -> t.equalsIgnoreCase(name));
    }

    @Provide
    Arbitrary<String> unknownColumnNames() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(10)
            .ofMaxLength(30)
            .filter(s -> !isKnownColumn(s) && !s.isBlank());
    }

    @Provide
    Arbitrary<String> unknownTableNames() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(10)
            .ofMaxLength(30)
            .filter(s -> !isKnownTable(s) && !s.isBlank());
    }

    @Provide
    Arbitrary<String> usersColumns() {
        return Arbitraries.of("id", "name", "email", "active");
    }

    @Provide
    Arbitrary<String> ordersColumns() {
        return Arbitraries.of("id", "user_id", "total", "status");
    }

    @Provide
    Arbitrary<String> nonAmbiguousUsersColumns() {
        return Arbitraries.of("name", "email", "active");
    }

    @Provide
    Arbitrary<String> nonAmbiguousOrdersColumns() {
        return Arbitraries.of("user_id", "total", "status");
    }

    @Provide
    Arbitrary<Class<?>> wrongTypesForBigint() {
        return Arbitraries.of(String.class, Boolean.class, Float.class);
    }

    @Provide
    Arbitrary<Class<?>> correctTypesForBigint() {
        return Arbitraries.of(Long.class);
    }

    @Property
    void select_unknownColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_unknownColumnInWhere_alwaysFails(@ForAll("unknownColumnNames") String col) {
        var where = new io.github.hacihaciyev.sql.expressions.BinaryOp(
            io.github.hacihaciyev.sql.expressions.BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base(col),
            new io.github.hacihaciyev.sql.expressions.Literal.IntLiteral(1)
        );

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.of(where),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_unknownColumnInGroupBy_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(new ColumnRef.Base(col)),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_unknownColumnInOrderBy_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(new ColumnRef.Base(col)),
            Optional.empty()
        ));
    }

    @Property
    void select_knownNonAmbiguousUsersColumn_alwaysPasses(@ForAll("nonAmbiguousUsersColumns") String col) {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_knownNonAmbiguousOrdersColumn_alwaysPasses(@ForAll("nonAmbiguousOrdersColumns") String col) {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("orders")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_qualifiedColumns_alwaysResolveAmbiguity(
            @ForAll("nonAmbiguousUsersColumns") String uCol,
            @ForAll("nonAmbiguousOrdersColumns") String oCol
    ) {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users", "u"), physical("orders", "o")),
            List.of(aliasedRef("u", uCol, "u_" + uCol), aliasedRef("o", oCol, "o_" + oCol)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_unknownTable_alwaysFails(@ForAll("unknownTableNames") String table) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical(table)),
            List.of(namedRef("id")),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_wrongTypeForBigint_alwaysFails(@ForAll("wrongTypesForBigint") Class<?> type) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("id", type)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_correctTypeForBigint_alwaysPasses(@ForAll("correctTypesForBigint") Class<?> type) {
        assertDoesNotThrow(() -> ContextFactory.selectContext(
            List.of(physical("users")),
            List.of(namedRef("id", type)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void insert_unknownColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void insert_knownColumn_alwaysPasses(@ForAll("nonAmbiguousUsersColumns") String col) {
        assertDoesNotThrow(() -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void insert_unknownReturningColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.insertContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(namedRef(col)),
            Optional.empty()
        ));
    }

    @Property
    void update_unknownColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void update_knownColumn_alwaysPasses(@ForAll("nonAmbiguousUsersColumns") String col) {
        assertDoesNotThrow(() -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void update_unknownWhereColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        var where = new io.github.hacihaciyev.sql.expressions.BinaryOp(
            io.github.hacihaciyev.sql.expressions.BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base(col),
            new io.github.hacihaciyev.sql.expressions.Literal.IntLiteral(1)
        );

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.of(where),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void update_unknownReturningColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.updateContext(
            List.of(physical("users")),
            List.of(namedRef("name")),
            Optional.empty(),
            List.of(namedRef(col)),
            Optional.empty()
        ));
    }

    @Property
    void delete_unknownWhereColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        var where = new io.github.hacihaciyev.sql.expressions.BinaryOp(
            io.github.hacihaciyev.sql.expressions.BinaryOp.BinaryOperator.EQ,
            new ColumnRef.Base(col),
            new io.github.hacihaciyev.sql.expressions.Literal.IntLiteral(1)
        );

        assertThrows(SchemaVerificationException.class, () -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.of(where),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void delete_unknownReturningColumn_alwaysFails(@ForAll("unknownColumnNames") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.deleteContext(
            List.of(physical("users")),
            Optional.empty(),
            List.of(namedRef(col)),
            Optional.empty()
        ));
    }

    @Property
    void delete_unknownTable_alwaysFails(@ForAll("unknownTableNames") String table) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.deleteContext(
            List.of(physical(table)),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }

    @Property
    void select_duplicateTableNames_alwaysFails(@ForAll("usersColumns") String col) {
        assertThrows(SchemaVerificationException.class, () -> ContextFactory.selectContext(
            List.of(physical("users"), physical("users")),
            List.of(namedRef(col)),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }
}