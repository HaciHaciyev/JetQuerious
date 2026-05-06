package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.internal.value_objects.Context;
import io.github.hacihaciyev.sql.internal.value_objects.ContextFactory;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;

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
        private final List<ColumnEntry> columns;

        ColumnsStage(Object[] pairs) {
            pairs = requireNonNull(pairs, "Column pairs cannot be null").clone();
            if (pairs.length == 0) throw new IllegalArgumentException("At least one column is required");
            if (pairs.length % 2 != 0) throw new IllegalArgumentException("Column pairs must be even: (name, type, ...)");

            var columns = new java.util.ArrayList<ColumnEntry>();

            for (var i = 0; i < pairs.length; i += 2) columns.add(switch (pairs[i]) {
                
                case String name when !name.isBlank() -> switch (pairs[i + 1]) {
                    case Class<?> type -> new ColumnEntry(new ColumnRef.Base(name.trim()), type);
                    default -> throw new IllegalArgumentException("Expected Class at index " + (i + 1) + ", got: " + pairs[i + 1]);
                };
                
                case String _ -> throw new IllegalArgumentException("Column name cannot be blank at index " + i);
                
                case ColumnRef ref -> switch (pairs[i + 1]) {
                    case Class<?> type -> new ColumnEntry(ref, type);
                    default -> throw new IllegalArgumentException("Expected Class at index " + (i + 1) + ", got: " + pairs[i + 1]);
                };
                
                default -> throw new IllegalArgumentException("Expected String or ColumnRef at index " + i + ", got: " + pairs[i]);
            });

            this.columns = List.copyOf(columns);
        }

        public ConflictTargetStage onConflict(ColumnRef... conflictColumns) {
            return new ConflictTargetStage(columns, List.of(conflictColumns));
        }

        public ConflictTargetStage onConflict(String... conflictColumns) {
            return onConflict(Arrays.stream(conflictColumns).map(ColumnRef.Base::new).toArray(ColumnRef[]::new));
        }

        public BuildStage returning(ColumnRef... exprs) {
            return new BuildStage(columns, null, List.of(exprs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames).map(ColumnRef.Base::new).toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(columns, null, List.of()).build();
        }
    }

    public final class ConflictTargetStage {
        private final List<ColumnEntry> columns;
        private final List<ColumnRef> conflictColumns;

        ConflictTargetStage(List<ColumnEntry> columns, List<ColumnRef> conflictColumns) {
            this.columns         = columns;
            this.conflictColumns = conflictColumns;
        }

        public ReturningStage doNothing() {
            return new ReturningStage(columns, new Conflict(conflictColumns, new ConflictAction.DoNothing()));
        }

        public ReturningStage updateAll() {
            return new ReturningStage(columns, new Conflict(conflictColumns, new ConflictAction.DoUpdate(List.of())));
        }

        public ReturningStage update(ColumnRef... updateColumns) {
            return new ReturningStage(columns, new Conflict(conflictColumns, new ConflictAction.DoUpdate(List.of(updateColumns))));
        }

        public ReturningStage update(String... updateColumns) {
            return update(Arrays.stream(updateColumns).map(ColumnRef.Base::new).toArray(ColumnRef[]::new));
        }
    }

    public final class ReturningStage {
        private final List<ColumnEntry> columns;
        private final Conflict conflict;

        ReturningStage(List<ColumnEntry> columns, Conflict conflict) {
            this.columns  = columns;
            this.conflict = conflict;
        }

        public BuildStage returning(ColumnRef... exprs) {
            return new BuildStage(columns, conflict, List.of(exprs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames).map(ColumnRef.Base::new).toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(columns, conflict, List.of()).build();
        }
    }

    public final class BuildStage {
        private final List<ColumnEntry> columns;
        private final Conflict conflict;
        private final List<ColumnRef> returning;

        BuildStage(List<ColumnEntry> columns, Conflict conflict, List<ColumnRef> returning) {
            this.columns   = columns;
            this.conflict  = conflict;
            this.returning = returning;
        }

        public JQ.Write build() {
            return new JQ.Write(buildSql(), buildContext());
        }

        private String buildSql() {
            var colNames = columns.stream().map(e -> e.ref().name()).toList();
            var sb = new StringBuilder();

            sb.append("INSERT INTO ").append(tref)
              .append(" (").append(String.join(", ", colNames)).append(")")
              .append(" VALUES (").append("?, ".repeat(columns.size() - 1)).append("?)");

            if (conflict != null) {
                sb.append(" ON CONFLICT (")
                  .append(String.join(", ", conflict.conflictColumns().stream().map(ColumnRef::name).toList()))
                  .append(")");

                switch (conflict.action()) {
                    case ConflictAction.DoNothing _ -> sb.append(" DO NOTHING");
                    case ConflictAction.DoUpdate a -> {
                        var updateCols = a.updateColumns().isEmpty() ? colNames : a.updateColumns().stream().map(ColumnRef::name).toList();
                        sb.append(" DO UPDATE SET ")
                          .append(String.join(", ", updateCols.stream().map(c -> c + " = EXCLUDED." + c).toList()));
                    }
                }
            }

            if (!returning.isEmpty())
                sb.append(" RETURNING ").append(String.join(", ", returning.stream().map(ColumnRef::name).toList()));

            return sb.toString();
        }

        private Context.Insert buildContext() {
            var source = new TableSource.Physical(tref);

            var refs = columns.stream().map(e -> (Ref) switch (e.ref()) {
                case ColumnRef.Base(var name, _) ->
                    new Ref.Named(new Projection.Base(new ColumnRef.Base(name, new ColumnRef.Type.Some(e.type_()))));
               
                case ColumnRef ref -> new Ref.Named(new Projection.Base(ref));
            }).toList();

            var returningRefs = Optional.of(returning)
                .filter(r -> !r.isEmpty())
                .map(r -> r.stream().map(e -> (Ref) new Ref.Named(new Projection.Base(e))).toList());

            return ContextFactory.insertContext(List.of(source), refs, returningRefs, Optional.empty());
        }
    }

    private record ColumnEntry(ColumnRef ref, Class<?> type_) {
        public ColumnEntry {
            requireNonNull(ref);
            requireNonNull(type_);
        }
    }

    private record Conflict(List<ColumnRef> conflictColumns, ConflictAction action) {
        public Conflict {
            conflictColumns = List.copyOf(requireNonNull(conflictColumns, "Conflict columns cannot be null"));
            requireNonNull(action, "Conflict action cannot be null");
            if (conflictColumns.isEmpty()) throw new IllegalArgumentException("At least one conflict column is required");
        }
    }

    private sealed interface ConflictAction {
        record DoNothing() implements ConflictAction {}
        record DoUpdate(List<ColumnRef> updateColumns) implements ConflictAction {
            public DoUpdate {
                updateColumns = List.copyOf(requireNonNull(updateColumns, "Update columns cannot be null"));
                for (var cref : updateColumns) requireNonNull(cref);
            }
        }
    }
}