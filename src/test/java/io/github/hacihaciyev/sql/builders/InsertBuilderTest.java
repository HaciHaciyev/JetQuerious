package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.util.DBTestContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DBTestContainer.class)
class InsertBuilderTest {

    @Test
    void singleColumn() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class)
            .build();

        assertInstanceOf(JQ.Write.class, jq);
        assertEquals("INSERT INTO users (name) VALUES (?)", jq.sql());
    }

    @Test
    void multipleColumns() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class, "active", Boolean.class)
            .build();

        assertEquals("INSERT INTO users (name, email, active) VALUES (?, ?, ?)", jq.sql());
    }

    @Test
    void withTableRef() {
        var jq = new InsertBuilder(new TableRef.Base("users"))
            .columns("name", String.class)
            .build();

        assertEquals("INSERT INTO users (name) VALUES (?)", jq.sql());
    }

    @Test
    void withReturning_strings() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class)
            .returning("id", "name")
            .build();

        assertEquals("INSERT INTO users (name) VALUES (?) RETURNING id, name", jq.sql());
    }

    @Test
    void withReturning_afterConflict() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class)
            .onConflict("email")
            .doNothing()
            .returning("id")
            .build();

        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO NOTHING RETURNING id", jq.sql());
    }

    @Test
    void onConflict_doNothing() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class)
            .onConflict("email")
            .doNothing()
            .build();

        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO NOTHING", jq.sql());
    }

    @Test
    void onConflict_doNothing_multipleConflictColumns() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class)
            .onConflict("name", "email")
            .doNothing()
            .build();

        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (name, email) DO NOTHING", jq.sql());
    }

    @Test
    void onConflict_updateAll() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class)
            .onConflict("email")
            .updateAll()
            .build();
    
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name", jq.sql());
    }

    @Test
    void onConflict_update_specificColumns() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class)
            .onConflict("email")
            .update("name")
            .build();

        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name", jq.sql());
    }

    @Test
    void onConflict_update_withReturning() {
        var jq = new InsertBuilder("users")
            .columns("name", String.class, "email", String.class)
            .onConflict("email")
            .update("name")
            .returning("id")
            .build();

        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name RETURNING id", jq.sql());
    }

    @Test
    void nullTableRef_throws() {
        assertThrows(NullPointerException.class, () -> new InsertBuilder((TableRef) null));
    }

    @Test
    void emptyColumns_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new InsertBuilder("users").columns());
    }

    @Test
    void oddColumnPairs_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new InsertBuilder("users").columns("name"));
    }

    @Test
    void blankColumnName_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new InsertBuilder("users").columns("  ", String.class));
    }

    @Test
    void wrongTypeForColumn_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new InsertBuilder("users").columns("name", "notAClass"));
    }

    @Test
    void nonExistentTable_throws() {
        assertThrows(Exception.class, () ->
            new InsertBuilder("ghost_table")
                .columns("name", String.class)
                .build());
    }

    @Test
    void nonExistentColumn_throws() {
        assertThrows(Exception.class, () ->
            new InsertBuilder("users")
                .columns("ghost_column", String.class)
                .build());
    }
    
    @Test
    void nonExistentColumnInReturning_throws() {
        assertThrows(Exception.class, () ->
            new InsertBuilder("users")
                .columns("name", String.class)
                .returning("ghost_column")
                .build());
    }
    
    @Nested
    class OnConflictValidation {

        @Test
        void onConflict_updateConflictColumn_throws() {
            assertThrows(Exception.class, () ->
                    new InsertBuilder("users")
                            .columns("name", String.class, "email", String.class)
                            .onConflict("email")
                            .update("email")
                            .build()
            );
        }

        @Test
        void onConflict_updateNonConflictColumn_passes() {
            assertDoesNotThrow(() ->
                    new InsertBuilder("users")
                            .columns("name", String.class, "email", String.class)
                            .onConflict("email")
                            .update("name")
                            .build()
            );
        }

        @Test
        void onConflict_updateAll_excludesConflictColumns() {
            assertThrows(Exception.class, () ->
                    new InsertBuilder("users")
                            .columns("email", String.class)
                            .onConflict("email")
                            .updateAll()
                            .build()
            );
        }

        @Test
        void onConflict_multipleConflictCols_updateOneOfThem_throws() {
            assertThrows(Exception.class, () ->
                    new InsertBuilder("users")
                            .columns("name", String.class, "email", String.class)
                            .onConflict("name", "email")
                            .update("name")
                            .build()
            );
        }

        @Test
        void onConflict_caseInsensitiveConflictColumn_throws() {
            assertThrows(Exception.class, () ->
                    new InsertBuilder("users")
                            .columns("name", String.class, "email", String.class)
                            .onConflict("EMAIL")
                            .update("email")
                            .build()
            );
        }
    }

    @Nested
    class ConflictColumnSchemaValidation {

        @Test
        void onConflict_nonExistentConflictColumn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    new InsertBuilder("users")
                            .columns("name", String.class)
                            .onConflict("ghost_column")
                            .doNothing()
                            .build()
            );
        }

        @Test
        void onConflict_nonExistentUpdateColumn_throws() {
            assertThrows(SchemaVerificationException.class, () ->
                    new InsertBuilder("users")
                            .columns("name", String.class, "email", String.class)
                            .onConflict("email")
                            .update("ghost_column")
                            .build()
            );
        }
    }
}