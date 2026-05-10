package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.internal.builders.DeleteSQL;
import io.github.hacihaciyev.sql.internal.value_objects.Ref;
import io.github.hacihaciyev.sql.internal.value_objects.TableSource;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class DeleteBuilder {
    private final TableRef tref;

    public DeleteBuilder(TableRef tref) {
        this.tref = requireNonNull(tref, "Table reference cannot be null");
    }

    public DeleteBuilder(String tref) {
        this(new TableRef.Base(tref));
    }

    public WhereStage where(Expr condition) {
        return new WhereStage(requireNonNull(condition, "WHERE condition cannot be null"));
    }

    public BuildStage returning(ColumnRef... crefs) {
        return new BuildStage(null, List.of(crefs));
    }

    public BuildStage returning(String... columnNames) {
        return returning(Arrays.stream(columnNames)
            .map(ColumnRef.Base::new)
            .toArray(ColumnRef[]::new));
    }

    public JQ.Write build() {
        return new BuildStage(null, List.of()).build();
    }
    
    public JQ.Write build(JQ.Read outer) {
        return new BuildStage(null, List.of()).build(outer);
    }

    public final class WhereStage {
        private final Expr where;

        WhereStage(Expr where) {
            this.where = where;
        }

        public BuildStage returning(ColumnRef... crefs) {
            return new BuildStage(where, List.of(crefs));
        }

        public BuildStage returning(String... columnNames) {
            return returning(Arrays.stream(columnNames)
                .map(ColumnRef.Base::new)
                .toArray(ColumnRef[]::new));
        }

        public JQ.Write build() {
            return new BuildStage(where, List.of()).build();
        }
        
        public JQ.Write build(JQ.Read outer) {
            return new BuildStage(where, List.of()).build(outer);
        }
    }

    public final class BuildStage {
        private final Expr            where;
        private final List<ColumnRef> returning;

        BuildStage(Expr where, List<ColumnRef> returning) {
            this.where     = where;
            this.returning = returning;
        }

        public JQ.Write build() {
            return new JQ.Write(buildSql(), buildContext(Optional.empty()));
        }
    
        public JQ.Write build(JQ.Read outer) {
            return new JQ.Write(buildSql(), buildContext(Optional.of((Context) outer.context())));
        }
            
        private String buildSql() {
            var retNames = returning.stream().map(ColumnRef::name).toList();
            return DeleteSQL.build(tref, Optional.ofNullable(where), retNames);
        }
        
        private Context.Delete buildContext(Optional<Context> outer) {
            var source = new TableSource.Physical(tref);
    
            var returningRefs = returning.stream()
                .map(e -> (Ref) new Ref.Named(new Projection.Base(e)))
                .toList();
    
            return ContextFactory.deleteContext(
                List.of(source),
                Optional.ofNullable(where),
                returningRefs,
                outer
            );
        }
    }
}