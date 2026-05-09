package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.internal.value_objects.InsertEntry;
import io.github.hacihaciyev.sql.internal.builders.InsertSQL;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.internal.value_objects.OnConflict;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class InsertBuilder {
    private final TableRef tref;

    public InsertBuilder(TableRef tref) {
        this.tref = requireNonNull(tref, "Table reference cannot be null");
    }

    public InsertBuilder(String tref) {
        this(new TableRef.Base(tref));
    }

    public ColumnsStage columns(Object... pairs) {
        return new ColumnsStage(pairs);
    }

    public final class ColumnsStage {
        private final List<InsertEntry> columns;

        ColumnsStage(Object[] pairs) {
            pairs = requireNonNull(pairs, "Column pairs cannot be null").clone();
            if (pairs.length == 0) throw new IllegalArgumentException("At least one column is required");
            if (pairs.length % 2 != 0) throw new IllegalArgumentException("Column pairs must be even: (name, type, ...)");

            var columns = new ArrayList<InsertEntry>();

            for (var i = 0; i < pairs.length; i += 2) {
                var name = switch (pairs[i]) {
                    case String s when !s.isBlank() -> new ColumnRef.Base(s.trim());
                    case ColumnRef.Base b -> b;
                    case String _ -> throw new IllegalArgumentException("Column name cannot be blank at index " + i);
                    default -> throw new IllegalArgumentException("Expected String at index " + i + ", got: " + pairs[i]);
                };

                var type_ = switch (pairs[i + 1]) {
                    case Class<?> c -> c;
                    default -> throw new IllegalArgumentException("Expected Class at index " + (i + 1) + ", got: " + pairs[i + 1]);
                };

                columns.add(InsertEntry.of(name, type_));
            }

            this.columns = List.copyOf(columns);
        }

        public ConflictTargetStage onConflict(ColumnRef.Base... conflictColumns) {
            return new ConflictTargetStage(columns, List.of(conflictColumns));
        }

        public ConflictTargetStage onConflict(String... conflictColumns) {
            return onConflict(Arrays.stream(conflictColumns)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef.Base[]::new));
        }

        public BuildStage returning(ColumnRef... crefs) {
            return new BuildStage(columns, null, List.of(crefs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(columns, null, List.of()).build();
        }
    }

    public final class ConflictTargetStage {
        private final List<InsertEntry> columns;
        private final List<ColumnRef.Base> conflictColumns;

        ConflictTargetStage(List<InsertEntry> columns, List<ColumnRef.Base> conflictColumns) {
            this.columns         = columns;
            this.conflictColumns = conflictColumns;
        }

        public ReturningStage doNothing() {
            return new ReturningStage(columns, OnConflict.doNothing(conflictColumns));
        }

        public ReturningStage updateAll() {
            var updateCols = columns.stream()
                .map(InsertEntry::col)
                .filter(col -> conflictColumns.stream().noneMatch(c -> c.name().equalsIgnoreCase(col.name())))
                .toList();
                
            return new ReturningStage(columns, OnConflict.updateSet(conflictColumns, updateCols));
        }
        
        public ReturningStage update(ColumnRef.Base... updateColumns) {
            return new ReturningStage(columns, OnConflict.updateSet(conflictColumns, List.of(updateColumns)));
        }

        public ReturningStage update(String... updateColumns) {
            return update(Arrays.stream(updateColumns)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef.Base[]::new));
        }
    }

    public final class ReturningStage {
        private final List<InsertEntry> columns;
        private final OnConflict conflict;

        ReturningStage(List<InsertEntry> columns, OnConflict conflict) {
            this.columns  = columns;
            this.conflict = conflict;
        }

        public BuildStage returning(ColumnRef... crefs) {
            return new BuildStage(columns, conflict, List.of(crefs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(columns, conflict, List.of()).build();
        }
    }

    public final class BuildStage {
        private final List<InsertEntry> columns;
        private final OnConflict conflict;
        private final List<ColumnRef> returning;

        BuildStage(List<InsertEntry> columns, OnConflict conflict, List<ColumnRef> returning) {
            this.columns   = columns;
            this.conflict  = conflict;
            this.returning = returning;
        }

        public JQ.Write build() {
            return new JQ.Write(buildSql(), buildContext());
        }

        private String buildSql() {
            var retNames = returning.stream().map(ColumnRef::name).toList();
            return InsertSQL.build(tref, columns, Optional.ofNullable(conflict), retNames);
        }

        private Context.Insert buildContext() {
            var source = new TableSource.Physical(tref);

            var refs = columns.stream()
                .map(e -> (Ref) new Ref.Named(new Projection.Base(new ColumnRef.Base(e.col().name(), new ColumnRef.Type.Some(e.type_())))))
                .toList();

            var returningRefs = returning.stream()
                .map(e -> (Ref) new Ref.Named(new Projection.Base(e)))
                .toList();

            return ContextFactory.insertContext(
                List.of(source),
                refs,
                Optional.ofNullable(conflict),
                returningRefs,
                Optional.empty()
            );
        }
    }
}