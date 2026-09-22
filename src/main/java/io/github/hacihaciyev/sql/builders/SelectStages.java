package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.internal.value_objects.ForUpdate;
import io.github.hacihaciyev.sql.internal.value_objects.FromSource;
import io.github.hacihaciyev.sql.internal.value_objects.JoinEntry;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SelectStages {

    private SelectStages() {}

    @FunctionalInterface
    public interface Finisher<T> {
        T finish(QuerySpec spec);
    }

    public static <T> SelectStage<T> select(List<Projection> projections, boolean distinct, Finisher<T> finisher) {
        requireNonNull(projections, "Projections cannot be null");
        requireNonNull(finisher, "Finisher cannot be null");
        return new SelectStage<>(List.copyOf(projections), distinct, finisher);
    }

        public static final class SelectStage<T> {
        private final List<Projection> projections;
        private final boolean distinct;
        private final Finisher<T> finisher;

        private SelectStage(List<Projection> projections, boolean distinct, Finisher<T> finisher) {
            this.projections = projections;
            this.distinct    = distinct;
            this.finisher    = finisher;
        }

        public Stage<T> from(TableRef tref) {
            return stageOf(new FromSource.Physical(requireNonNull(tref, "Table reference cannot be null")));
        }

        public Stage<T> from(String table) {
            return from(new TableRef.Base(requireNonNull(table, "Table name cannot be null")));
        }

        public Stage<T> from(JQ.Read subquery, String alias) {
            return stageOf(new FromSource.Subquery(
                requireNonNull(subquery, "Subquery cannot be null"),
                requireNonNull(alias, "Subquery alias cannot be null")
            ));
        }

        private Stage<T> stageOf(FromSource source) {
            return new Stage<>(new QuerySpec(
                projections, distinct, source, List.of(),
                null, List.of(), null, List.of(),
                null, null, Optional.empty()
            ), finisher);
        }
    }

    public static final class Stage<T> {
        private final QuerySpec spec;
        private final Finisher<T> finisher;

        private Stage(QuerySpec spec, Finisher<T> finisher) {
            this.spec     = spec;
            this.finisher = finisher;
        }

        public Stage<T> join(TableRef tref, Expr on) {
            return withJoin(JoinEntry.inner(physical(tref), requireOn(on)));
        }

        public Stage<T> join(String table, Expr on) {
            return join(new TableRef.Base(table), on);
        }

        public Stage<T> join(JQ.Read subquery, String alias, Expr on) {
            return withJoin(JoinEntry.inner(subquery(subquery, alias), requireOn(on)));
        }

        public Stage<T> leftJoin(TableRef tref, Expr on) {
            return withJoin(JoinEntry.left(physical(tref), requireOn(on)));
        }

        public Stage<T> leftJoin(String table, Expr on) {
            return leftJoin(new TableRef.Base(table), on);
        }

        public Stage<T> leftJoin(JQ.Read subquery, String alias, Expr on) {
            return withJoin(JoinEntry.left(subquery(subquery, alias), requireOn(on)));
        }

        public Stage<T> rightJoin(TableRef tref, Expr on) {
            return withJoin(JoinEntry.right(physical(tref), requireOn(on)));
        }

        public Stage<T> rightJoin(String table, Expr on) {
            return rightJoin(new TableRef.Base(table), on);
        }

        public Stage<T> fullJoin(TableRef tref, Expr on) {
            return withJoin(JoinEntry.full(physical(tref), requireOn(on)));
        }

        public Stage<T> fullJoin(String table, Expr on) {
            return fullJoin(new TableRef.Base(table), on);
        }

        public Stage<T> crossJoin(TableRef tref) {
            return withJoin(JoinEntry.cross(physical(tref)));
        }

        public Stage<T> crossJoin(String table) {
            return crossJoin(new TableRef.Base(table));
        }

        public Stage<T> where(Expr condition) {
            return next(spec.withWhere(requireNonNull(condition, "WHERE condition cannot be null")));
        }

        public Stage<T> groupBy(Expr... exprs) {
            return next(spec.withGroupBy(exprList(exprs, "GROUP BY")));
        }

        public Stage<T> groupBy(String... columns) {
            return groupBy(toColumnRefs(columns));
        }

        public Stage<T> having(Expr condition) {
            return next(spec.withHaving(requireNonNull(condition, "HAVING condition cannot be null")));
        }

        public Stage<T> orderBy(Expr... exprs) {
            return next(spec.withOrderBy(exprList(exprs, "ORDER BY")));
        }

        public Stage<T> orderBy(String... columns) {
            return orderBy(toColumnRefs(columns));
        }

        public Stage<T> limit(int value) {
            return next(spec.withLimit(new Limit(value)));
        }

        public Stage<T> offset(int value) {
            return next(spec.withOffset(new Offset(value)));
        }

        public Stage<T> forUpdate(ForUpdate forUpdate) {
            return next(spec.withForUpdate(Optional.of(requireNonNull(forUpdate, "FOR UPDATE cannot be null"))));
        }

        public Stage<T> forUpdate() {
            return forUpdate(ForUpdate.simple());
        }

        public T build() {
            return finisher.finish(spec);
        }

        private Stage<T> withJoin(JoinEntry entry) {
            var joins = new ArrayList<>(spec.joins());
            joins.add(entry);
            return next(spec.withJoins(List.copyOf(joins)));
        }

        private Stage<T> next(QuerySpec updated) {
            return new Stage<>(updated, finisher);
        }

        private static FromSource physical(TableRef tref) {
            return new FromSource.Physical(requireNonNull(tref, "Join table reference cannot be null"));
        }

        private static FromSource subquery(JQ.Read query, String alias) {
            return new FromSource.Subquery(
                requireNonNull(query, "Join subquery cannot be null"),
                requireNonNull(alias, "Join subquery alias cannot be null")
            );
        }

        private static Expr requireOn(Expr on) {
            return requireNonNull(on, "JOIN ON condition cannot be null");
        }

        private static List<Expr> exprList(Expr[] exprs, String clause) {
            requireNonNull(exprs, clause + " expressions cannot be null");
            if (exprs.length == 0) throw new IllegalArgumentException("At least one " + clause + " expression is required");
            for (var e : exprs) requireNonNull(e, clause + " expression cannot be null");
            return List.of(exprs);
        }

        private static Expr[] toColumnRefs(String... columns) {
            requireNonNull(columns, "Columns cannot be null");
            return Arrays.stream(columns)
                .map(c -> (Expr) new ColumnRef.Base(requireNonNull(c, "Column name cannot be null")))
                .toArray(Expr[]::new);
        }
    }
}