package io.github.hacihaciyev.sql.internal.schema;

import io.github.hacihaciyev.types.SQLType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnTest {

    @Test
    @DisplayName("Should create known column with all properties")
    void shouldCreateKnownColumn() {
        var column = new Column.Known("user_id", SQLType.BIGINT, false);

        assertEquals("user_id", column.name());
        assertEquals(SQLType.BIGINT, column.type());
        assertFalse(column.nullable());
        assertTrue(column.known());
    }

    @Test
    @DisplayName("Should create nullable known column")
    void shouldCreateNullableKnownColumn() {
        var column = new Column.Known("email", SQLType.VARCHAR, true);

        assertTrue(column.nullable());
        assertTrue(column.known());
    }

    @Test
    @DisplayName("Should trim whitespace from known column name")
    void shouldTrimWhitespaceFromKnownColumnName() {
        var column = new Column.Known("  user_id  ", SQLType.INTEGER, false);

        assertEquals("user_id", column.name());
    }

    @Test
    @DisplayName("Should throw NPE when known column name is null")
    void shouldThrowNPEWhenKnownColumnNameIsNull() {
        assertThrows(NullPointerException.class, () ->
                new Column.Known(null, SQLType.INTEGER, false)
        );
    }

    @Test
    @DisplayName("Should throw NPE when known column type is null")
    void shouldThrowNPEWhenKnownColumnTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
                new Column.Known("id", null, false)
        );
    }

    @Test
    @DisplayName("Known column toString should return name")
    void knownColumnToStringShouldReturnName() {
        var column = new Column.Known("user_id", SQLType.BIGINT, false);

        assertEquals("user_id", column.toString());
    }

    @Test
    @DisplayName("Should create unknown column")
    void shouldCreateUnknownColumn() {
        var column = new Column.Unknown("exotic_column", true);

        assertEquals("exotic_column", column.name());
        assertTrue(column.nullable());
        assertFalse(column.known());
    }

    @Test
    @DisplayName("Should create non-nullable unknown column")
    void shouldCreateNonNullableUnknownColumn() {
        var column = new Column.Unknown("mystery_col", false);

        assertFalse(column.nullable());
        assertFalse(column.known());
    }

    @Test
    @DisplayName("Should trim whitespace from unknown column name")
    void shouldTrimWhitespaceFromUnknownColumnName() {
        var column = new Column.Unknown("  unknown_col  ", true);

        assertEquals("unknown_col", column.name());
    }

    @Test
    @DisplayName("Should throw NPE when unknown column name is null")
    void shouldThrowNPEWhenUnknownColumnNameIsNull() {
        assertThrows(NullPointerException.class, () ->
                new Column.Unknown(null, false)
        );
    }

    @Test
    @DisplayName("Unknown column toString should return name")
    void unknownColumnToStringShouldReturnName() {
        var column = new Column.Unknown("exotic_type", true);

        assertEquals("exotic_type", column.toString());
    }

    @Test
    @DisplayName("Known columns with same values should be equal")
    void knownColumnsWithSameValuesShouldBeEqual() {
        var col1 = new Column.Known("id", SQLType.INTEGER, false);
        var col2 = new Column.Known("id", SQLType.INTEGER, false);

        assertEquals(col1, col2);
        assertEquals(col1.hashCode(), col2.hashCode());
    }

    @Test
    @DisplayName("Known columns with different types should not be equal")
    void knownColumnsWithDifferentTypesShouldNotBeEqual() {
        var col1 = new Column.Known("id", SQLType.INTEGER, false);
        var col2 = new Column.Known("id", SQLType.BIGINT, false);

        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Should support pattern matching on Column")
    void shouldSupportPatternMatchingOnColumn() {
        Column.Known known = new Column.Known("id", SQLType.INTEGER, false);
        Column.Unknown unknown = new Column.Unknown("exotic", true);

        Column.Known k1 = known;
        String knownResult = "Known: " + k1.name() + " (" + k1.type() + ")";
        assertEquals("Known: id (INTEGER)", knownResult);

        Column.Unknown u = unknown;
        String unknownResult = "Unknown: " + u.name();
        assertEquals("Unknown: exotic", unknownResult);
    }
}