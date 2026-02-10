package io.github.hacihaciyev.schema;

import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.dsl.TableRef;
import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import io.github.hacihaciyev.util.Result;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static java.util.Objects.requireNonNull;
import static io.github.hacihaciyev.schema.Table.Catalog;
import static io.github.hacihaciyev.schema.Table.Schema;

public class SchemaResolver {

    static final String TABLE_NOT_FOUND = "Table not found: catalog{%s}, schema{%s}, table{%s}";

    private static final String[] SCHEM_TYPES = new String[]{"TABLE", "VIEW"};

    private static final int CACHE_SIZE = Conf.INSTANCE.schemaCacheSize();

    private static final long TTL_NANOS = Conf.INSTANCE.schemaTTLInSeconds().toNanos();

    private static final AtomicReferenceArray<CachedTable> CACHE = new AtomicReferenceArray<>(CACHE_SIZE);

    private static final AtomicBoolean CLEANUP_RUNNING = new AtomicBoolean(true);

    private static final long CLEANUP_INTERVAL_MS = 1_000;

    static {
        Thread.startVirtualThread(() -> {
            while (CLEANUP_RUNNING.get()) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    cleanup();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private record CachedTable(Table table, long expiresAt, int dataSourceID) {
        boolean isExpired() {
            return System.nanoTime() > expiresAt;
        }

        boolean matches(String catalog, String schema, String tableName, int dataSourceId) {
            if (this.dataSourceID != dataSourceId) return false;
            if (!table.name().equals(tableName)) return false;

            var tableCat = asStrOrNull(table.catalog());
            var tableSchema = asStrOrNull(table.schema());

            return Objects.equals(tableCat, catalog) && Objects.equals(tableSchema, schema);
        }
    }

    private enum Meta {
        TABLE_CAT,
        TABLE_SCHEM,
        TABLE_NAME,
        COLUMN_NAME,
        TYPE_NAME,
        NULLABLE;

        @Override
        public String toString() {
            return name();
        }
    }

    private SchemaResolver() {}

    public static Result<Table, SchemaVerificationException> load(TableRef tableRef, DataSource dataSource) {
        requireNonNull(tableRef, "TableRef cannot be null");
        requireNonNull(dataSource, "DataSource cannot be null");

        var cat = catalogOrNull(tableRef);
        var schema = schemaOrNull(tableRef);
        var table = tableRef.name();

        var dataSourceId = System.identityHashCode(dataSource);
        var index = index(cat, schema, table, dataSourceId);

        var cache = CACHE.get(index);
        if (cache != null && cache.matches(cat, schema, table, dataSourceId)) {
            if (!cache.isExpired()) return new Ok<>(cache.table);
            CACHE.compareAndSet(index, cache, null);
        }

        try (Connection conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            var res = table(meta, cat, schema, table);

            if (res instanceof Ok(Table value)) {
                var expiresAt = System.nanoTime() + TTL_NANOS;
                CACHE.set(index, new CachedTable(value, expiresAt, dataSourceId));
            }

            return res;
        } catch (SQLException e) {
            return new Err<>(new SchemaVerificationException(TABLE_NOT_FOUND.formatted(cat, schema, table), e));
        }
    }

    private static void cleanup() {
        for (var i = 0; i < CACHE_SIZE; i++) {
            var cache = CACHE.get(i);
            if (cache != null && cache.isExpired()) CACHE.compareAndSet(i, cache, null);
        }
    }

    private static int index(String catalog, String schema, String table, int dataSourceId) {
        int h = 17;
        h = 31 * h + (catalog != null ? catalog.hashCode() : 0);
        h = 31 * h + (schema != null ? schema.hashCode() : 0);
        h = 31 * h + (table != null ? table.hashCode() : 0);
        h = 31 * h + dataSourceId;
        return (h & 0x7FFFFFFF) & (CACHE_SIZE - 1);
    }

    private static Result<Table, SchemaVerificationException> table(
            DatabaseMetaData meta, String cat, String schema, String table) throws SQLException {

        try (var tables = meta.getTables(cat, schema, table, SCHEM_TYPES)) {
            if (!tables.next()) return new Err<>(new SchemaVerificationException(TABLE_NOT_FOUND.formatted(cat, schema, table)));

            var actualCat = cat(tables);
            var actualSchem = schem(tables);
            var columns = loadColumns(meta, actualCat, actualSchem, table);

            if (columns.length == 0)
                return new Err<>(new SchemaVerificationException("Table has no accessible columns: " + table));

            return new Ok<>(new Table(actualCat, actualSchem, table, columns));
        }
    }

    private static Catalog cat(ResultSet tables) throws SQLException {
        var cat = tables.getString(Meta.TABLE_CAT.toString());
        if (cat == null) return new Catalog.Unknown();
        return new Catalog.Known(cat);
    }

    private static Schema schem(ResultSet tables) throws SQLException {
        var schem = tables.getString(Meta.TABLE_SCHEM.toString());
        if (schem == null) return new Schema.Unknown();
        return new Schema.Known(schem);
    }

    private static Column[] loadColumns(
            DatabaseMetaData metaData, Catalog catalog, Schema schema, String table) throws SQLException {

        var cols = new ArrayList<Column>();

        try (var rs = metaData.getColumns(asStrOrNull(catalog), asStrOrNull(schema), table, null)) {
            while (rs.next()) column(rs, cols);
        }

        return cols.toArray(new Column[0]);
    }

    private static void column(ResultSet rs, List<Column> cols) throws SQLException {
        var name = rs.getString(Meta.COLUMN_NAME.toString());
        var type = rs.getString(Meta.TYPE_NAME.toString());
        var nullable = rs.getInt(Meta.NULLABLE.toString()) == DatabaseMetaData.columnNullable;

        SQLType.parse(type).ifPresentOrElse(
                t -> cols.add(new Column.Known(name, t, nullable)),
                () -> cols.add(new Column.Unknown(name, nullable))
        );
    }

    private static String schemaOrNull(TableRef tableRef) {
        if (tableRef instanceof TableRef.SchemaRef sr) return sr.schema();
        return null;
    }

    private static String catalogOrNull(TableRef tableRef) {
        if (tableRef instanceof TableRef.CatalogRef cr) return cr.catalog();
        return null;
    }

    private static String asStrOrNull(Catalog catalog) {
        if (catalog instanceof Catalog.Known(var name)) return name;
        return null;
    }

    private static String asStrOrNull(Schema schema) {
        if (schema instanceof Schema.Known(var name)) return name;
        return null;
    }
}
