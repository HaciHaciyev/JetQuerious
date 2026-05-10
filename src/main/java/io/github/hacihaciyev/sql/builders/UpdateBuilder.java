package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.internal.ExprTraversal;
import io.github.hacihaciyev.sql.internal.builders.UpdateSQL;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.internal.value_objects.UpdateEntry;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static scala.jdk.javaapi.CollectionConverters.*;

public final class UpdateBuilder {
    private final TableRef tref;

    public UpdateBuilder(TableRef tref) {
        this.tref = requireNonNull(tref, "Table reference cannot be null");
    }

    public UpdateBuilder(String tref) {
        this(new TableRef.Base(tref));
    }

    public SetStage set(Object... pairs) {
        return new SetStage(pairs);
    }

    public final class SetStage {
        private final List<UpdateEntry> entries;

        SetStage(Object[] pairs) {
            pairs = requireNonNull(pairs, "Set pairs cannot be null").clone();
            if (pairs.length == 0) throw new IllegalArgumentException("At least one column is required");
            if (pairs.length % 2 != 0) throw new IllegalArgumentException("Set pairs must be even: (column, type|expr, ...)");

            var entries = new ArrayList<UpdateEntry>();

            for (var i = 0; i < pairs.length; i += 2) {
                var col = switch (pairs[i]) {
                    case String s when !s.isBlank() -> new ColumnRef.Base(s.trim());
                    case String _                   -> throw new IllegalArgumentException("Column name cannot be blank at index " + i);
                    case ColumnRef.Base b           -> b;
                    default -> throw new IllegalArgumentException("Expected String or ColumnRef.Base at index " + i + ", got: " + pairs[i]);
                };

                var entry = switch (pairs[i + 1]) {
                    case Class<?> c -> UpdateEntry.param(col, c);
                    case Expr e     -> UpdateEntry.computed(col, e);
                    default -> throw new IllegalArgumentException("Expected Class or Expr at index " + (i + 1) + ", got: " + pairs[i + 1]);
                };

                entries.add(entry);
            }

            this.entries = List.copyOf(entries);
        }

        public WhereStage where(Expr condition) {
            return new WhereStage(entries, requireNonNull(condition, "WHERE condition cannot be null"));
        }

        public BuildStage returning(ColumnRef... crefs) {
            return new BuildStage(entries, null, List.of(crefs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(entries, null, List.of()).build();
        }
    }

    public final class WhereStage {
        private final List<UpdateEntry> entries;
        private final Expr              where;

        WhereStage(List<UpdateEntry> entries, Expr where) {
            this.entries = entries;
            this.where   = where;
        }

        public BuildStage returning(ColumnRef... crefs) {
            return new BuildStage(entries, where, List.of(crefs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(entries, where, List.of()).build();
        }
    }

    public final class BuildStage {
        private final List<UpdateEntry> entries;
        private final Expr              where;
        private final List<ColumnRef>   returning;

        BuildStage(List<UpdateEntry> entries, Expr where, List<ColumnRef> returning) {
            this.entries   = entries;
            this.where     = where;
            this.returning = returning;
        }

        public JQ.Write build() {
            return new JQ.Write(buildSql(), buildContext());
        }

        private String buildSql() {
            var retNames = returning.stream().map(ColumnRef::name).toList();
            return UpdateSQL.build(tref, entries, Optional.ofNullable(where), retNames);
        }

        private Context.Update buildContext() {
            var source = new TableSource.Physical(tref);
        
            var refs = new java.util.ArrayList<Ref>();
            var setExprs = new java.util.ArrayList<Expr>();
        
            for (var entry : entries) switch (entry) {
                case UpdateEntry.Param p -> refs.add(new Ref.Named(new Projection.Base(new ColumnRef.Base(p.col().name(), new ColumnRef.Type.Some(p.type_())))));
    
                case UpdateEntry.Computed c -> {
                    refs.add(new Ref.Named(new Projection.Base(c.col())));
                    setExprs.add(c.expr());
                }
                
                default -> throw new IllegalArgumentException("Unexpected");
            }
        
            var returningRefs = returning.stream()
                .map(e -> (Ref) new Ref.Named(new Projection.Base(e)))
                .toList();
        
            return ContextFactory.updateContext(
                List.of(source),
                refs,
                setExprs,
                Optional.ofNullable(where),
                returningRefs,
                Optional.empty()
            );
        }
    }
}