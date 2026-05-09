package io.github.hacihaciyev.sql.internal.builders;

import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.internal.value_objects.InsertEntry;
import io.github.hacihaciyev.sql.internal.value_objects.OnConflict;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InsertSQLTest {

    private static InsertEntry entry(String col) {
        return InsertEntry.of(new ColumnRef.Base(col), String.class);
    }

    @Test
    void singleColumn() {
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name")),
            Optional.empty(),
            List.of()
        );
        assertEquals("INSERT INTO users (name) VALUES (?)", sql);
    }

    @Test
    void multipleColumns() {
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email"), entry("active")),
            Optional.empty(),
            List.of()
        );
        assertEquals("INSERT INTO users (name, email, active) VALUES (?, ?, ?)", sql);
    }

    @Test
    void withSchema() {
        var sql = InsertSQL.build(
            new TableRef.WithSchema("public", "users"),
            List.of(entry("name")),
            Optional.empty(),
            List.of()
        );
        assertEquals("INSERT INTO public.users (name) VALUES (?)", sql);
    }

    @Test
    void withAlias() {
        var sql = InsertSQL.build(
            new TableRef.AliasedBase("users", "u"),
            List.of(entry("name")),
            Optional.empty(),
            List.of()
        );
        assertEquals("INSERT INTO users AS u (name) VALUES (?)", sql);
    }

    @Test
    void withReturning_singleColumn() {
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name")),
            Optional.empty(),
            List.of("id")
        );
        assertEquals("INSERT INTO users (name) VALUES (?) RETURNING id", sql);
    }

    @Test
    void withReturning_multipleColumns() {
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name")),
            Optional.empty(),
            List.of("id", "name", "email")
        );
        assertEquals("INSERT INTO users (name) VALUES (?) RETURNING id, name, email", sql);
    }

    @Test
    void onConflict_doNothing_singleConflictColumn() {
        var conflict = OnConflict.doNothing(List.of(new ColumnRef.Base("email")));
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email")),
            Optional.of(conflict),
            List.of()
        );
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO NOTHING", sql);
    }

    @Test
    void onConflict_doNothing_multipleConflictColumns() {
        var conflict = OnConflict.doNothing(List.of(new ColumnRef.Base("name"), new ColumnRef.Base("email")));
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email")),
            Optional.of(conflict),
            List.of()
        );
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (name, email) DO NOTHING", sql);
    }

    @Test
    void onConflict_updateSet_singleColumn() {
        var conflict = OnConflict.updateSet(
            List.of(new ColumnRef.Base("email")),
            List.of(new ColumnRef.Base("name"))
        );
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email")),
            Optional.of(conflict),
            List.of()
        );
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name", sql);
    }

    @Test
    void onConflict_updateSet_multipleColumns() {
        var conflict = OnConflict.updateSet(
            List.of(new ColumnRef.Base("email")),
            List.of(new ColumnRef.Base("name"), new ColumnRef.Base("active"))
        );
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email"), entry("active")),
            Optional.of(conflict),
            List.of()
        );
        assertEquals("INSERT INTO users (name, email, active) VALUES (?, ?, ?) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name, active = EXCLUDED.active", sql);
    }

    @Test
    void onConflict_doNothing_withReturning() {
        var conflict = OnConflict.doNothing(List.of(new ColumnRef.Base("email")));
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email")),
            Optional.of(conflict),
            List.of("id")
        );
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO NOTHING RETURNING id", sql);
    }

    @Test
    void onConflict_updateSet_withReturning() {
        var conflict = OnConflict.updateSet(
            List.of(new ColumnRef.Base("email")),
            List.of(new ColumnRef.Base("name"))
        );
        var sql = InsertSQL.build(
            new TableRef.Base("users"),
            List.of(entry("name"), entry("email")),
            Optional.of(conflict),
            List.of("id", "name")
        );
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name RETURNING id, name", sql);
    }
}