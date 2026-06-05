package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.expressions.Subquery;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.internal.builders.SelectSQL;
import io.github.hacihaciyev.sql.internal.value_objects.*;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.Projection;
import io.github.hacihaciyev.sql.value_objects.TableRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SelectBuilder {
    
    private final List<Projection> projections;
    private final boolean          distinct;

    private SelectBuilder(boolean distinct, Projection... projections) {
        if (projections.length == 0) throw new IllegalArgumentException("At least one projection is required");
        for (var p : projections) requireNonNull(p, "Projection cannot be null");
        
        this.distinct    = distinct;
        this.projections = List.of(projections);
    }

    public static SelectBuilder select(Expr... exprs) {
        requireNonNull(exprs, "Expressions cannot be null");
        return new SelectBuilder(false, toProjections(exprs));
    }

    public static SelectBuilder selectDistinct(Expr... exprs) {
        requireNonNull(exprs, "Expressions cannot be null");
        return new SelectBuilder(true, toProjections(exprs));
    }
    
    public static SelectBuilder select(Projection... projections) {
        requireNonNull(projections, "Projections cannot be null");
        return new SelectBuilder(false, projections);
    }

    public static SelectBuilder selectDistinct(Projection... projections) {
        requireNonNull(projections, "Projections cannot be null");
        return new SelectBuilder(true, projections);
    }
    
    public static SelectBuilder selectAll() {
        return new SelectBuilder(false, new Projection.Wildcard());
    }
    
    public static SelectBuilder selectAllDistinct() {
        return new SelectBuilder(true, new Projection.Wildcard());
    }

    public static SelectBuilder selectAll(String... qualifiedWildcards) {
        requireNonNull(qualifiedWildcards, "Qualified wildcards cannot be null");
        return new SelectBuilder(false, toQW(qualifiedWildcards));
    }

    public static SelectBuilder selectAllDistinct(String... qualifiedWildcards) {
        requireNonNull(qualifiedWildcards, "Qualified wildcards cannot be null");
        return new SelectBuilder(true, toQW(qualifiedWildcards));
    }

    public FromStage from(TableRef tref) {
        return new FromStage(projections, distinct, new FromSource.Physical(requireNonNull(tref, "TableRef cannot be null")));
    }

    public FromStage from(String table) {
        return from(new TableRef.Base(table));
    }

    public FromStage from(Subquery.Table subquery, String alias) {
        requireNonNull(subquery, "Subquery cannot be null");
        requireNonNull(alias,    "Alias cannot be null");
        return new FromStage(projections, distinct, new FromSource.Subquery(subquery.jq(), alias));
    }

    public final class FromStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;

        FromStage(List<Projection> projections, boolean distinct, FromSource from) {
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
        }

        public JoinStage join(TableRef tref, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).join(tref, on);
        }

        public JoinStage join(String table, Expr on) {
            return join(new TableRef.Base(table), on);
        }

        public JoinStage join(Subquery.Table subquery, String alias, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).join(subquery, alias, on);
        }

        public JoinStage leftJoin(TableRef tref, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).leftJoin(tref, on);
        }

        public JoinStage leftJoin(String table, Expr on) {
            return leftJoin(new TableRef.Base(table), on);
        }

        public JoinStage leftJoin(Subquery.Table subquery, String alias, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).leftJoin(subquery, alias, on);
        }

        public JoinStage rightJoin(TableRef tref, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).rightJoin(tref, on);
        }

        public JoinStage rightJoin(String table, Expr on) {
            return rightJoin(new TableRef.Base(table), on);
        }

        public JoinStage rightJoin(Subquery.Table subquery, String alias, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).rightJoin(subquery, alias, on);
        }

        public JoinStage fullJoin(TableRef tref, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).fullJoin(tref, on);
        }

        public JoinStage fullJoin(String table, Expr on) {
            return fullJoin(new TableRef.Base(table), on);
        }

        public JoinStage fullJoin(Subquery.Table subquery, String alias, Expr on) {
            return new JoinStage(projections, distinct, from, List.of()).fullJoin(subquery, alias, on);
        }

        public JoinStage crossJoin(TableRef tref) {
            return new JoinStage(projections, distinct, from, List.of()).crossJoin(tref);
        }

        public JoinStage crossJoin(String table) {
            return crossJoin(new TableRef.Base(table));
        }

        public JoinStage crossJoin(Subquery.Table subquery, String alias) {
            return new JoinStage(projections, distinct, from, List.of()).crossJoin(subquery, alias);
        }

        public WhereStage where(Expr condition) {
            return new WhereStage(projections, distinct, from, List.of(),
                requireNonNull(condition, "WHERE condition cannot be null"));
        }

        public GroupByStage groupBy(Expr... exprs) {
            return new GroupByStage(projections, distinct, from, List.of(), null, List.of(exprs));
        }

        public GroupByStage groupBy(String... columns) {
            return groupBy(toColumnRefs(columns));
        }

        public OrderByStage orderBy(Expr... exprs) {
            return new OrderByStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(exprs));
        }

        public OrderByStage orderBy(String... columns) {
            return orderBy(toColumnRefs(columns));
        }

        public LimitStage limit(int limit) {
            return new LimitStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), new Limit(limit));
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, List.of(), null, List.of(), null, List.of(), null, null, Optional.empty()).build(outer);
        }
    }

    public final class JoinStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;
        private final List<JoinEntry>  joins;

        JoinStage(List<Projection> projections, boolean distinct, FromSource from, List<JoinEntry> joins) {
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
        }

        public JoinStage join(TableRef tref, Expr on) {
            return addJoin(JoinEntry.inner(new FromSource.Physical(requireNonNull(tref)), requireNonNull(on)));
        }

        public JoinStage join(String table, Expr on) {
            return join(new TableRef.Base(table), on);
        }

        public JoinStage join(Subquery.Table subquery, String alias, Expr on) {
            return addJoin(JoinEntry.inner(new FromSource.Subquery(requireNonNull(subquery.jq()), requireNonNull(alias)), requireNonNull(on)));
        }

        public JoinStage leftJoin(TableRef tref, Expr on) {
            return addJoin(JoinEntry.left(new FromSource.Physical(requireNonNull(tref)), requireNonNull(on)));
        }

        public JoinStage leftJoin(String table, Expr on) {
            return leftJoin(new TableRef.Base(table), on);
        }

        public JoinStage leftJoin(Subquery.Table subquery, String alias, Expr on) {
            return addJoin(JoinEntry.left(new FromSource.Subquery(requireNonNull(subquery.jq()), requireNonNull(alias)), requireNonNull(on)));
        }

        public JoinStage rightJoin(TableRef tref, Expr on) {
            return addJoin(JoinEntry.right(new FromSource.Physical(requireNonNull(tref)), requireNonNull(on)));
        }

        public JoinStage rightJoin(String table, Expr on) {
            return rightJoin(new TableRef.Base(table), on);
        }

        public JoinStage rightJoin(Subquery.Table subquery, String alias, Expr on) {
            return addJoin(JoinEntry.right(new FromSource.Subquery(requireNonNull(subquery.jq()), requireNonNull(alias)), requireNonNull(on)));
        }

        public JoinStage fullJoin(TableRef tref, Expr on) {
            return addJoin(JoinEntry.full(new FromSource.Physical(requireNonNull(tref)), requireNonNull(on)));
        }

        public JoinStage fullJoin(String table, Expr on) {
            return fullJoin(new TableRef.Base(table), on);
        }

        public JoinStage fullJoin(Subquery.Table subquery, String alias, Expr on) {
            return addJoin(JoinEntry.full(new FromSource.Subquery(requireNonNull(subquery.jq()), requireNonNull(alias)), requireNonNull(on)));
        }

        public JoinStage crossJoin(TableRef tref) {
            return addJoin(JoinEntry.cross(new FromSource.Physical(requireNonNull(tref))));
        }

        public JoinStage crossJoin(String table) {
            return crossJoin(new TableRef.Base(table));
        }

        public JoinStage crossJoin(Subquery.Table subquery, String alias) {
            return addJoin(JoinEntry.cross(new FromSource.Subquery(requireNonNull(subquery.jq()), requireNonNull(alias))));
        }

        public WhereStage where(Expr condition) {
            return new WhereStage(projections, distinct, from, joins,
                requireNonNull(condition, "WHERE condition cannot be null"));
        }

        public GroupByStage groupBy(Expr... exprs) {
            return new GroupByStage(projections, distinct, from, joins, null, List.of(exprs));
        }

        public GroupByStage groupBy(String... columns) {
            return groupBy(toColumnRefs(columns));
        }

        public OrderByStage orderBy(Expr... exprs) {
            return new OrderByStage(projections, distinct, from, joins, null, List.of(), null, List.of(exprs));
        }

        public OrderByStage orderBy(String... columns) {
            return orderBy(toColumnRefs(columns));
        }

        public LimitStage limit(int limit) {
            return new LimitStage(projections, distinct, from, joins, null, List.of(), null, List.of(), new Limit(limit));
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, joins, null, List.of(), null, List.of(), null, null, Optional.empty()).build(outer);
        }

        private JoinStage addJoin(JoinEntry entry) {
            var next = new ArrayList<>(joins);
            next.add(entry);
            return new JoinStage(projections, distinct, from, List.copyOf(next));
        }
    }

    public final class WhereStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;
        private final List<JoinEntry>  joins;
        private final Expr             where;

        WhereStage(List<Projection> projections, boolean distinct, FromSource from,
                   List<JoinEntry> joins, Expr where) {
            
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
            this.where       = where;
        }

        public GroupByStage groupBy(Expr... exprs) {
            return new GroupByStage(projections, distinct, from, joins, where, List.of(exprs));
        }

        public GroupByStage groupBy(String... columns) {
            return groupBy(toColumnRefs(columns));
        }

        public OrderByStage orderBy(Expr... exprs) {
            return new OrderByStage(projections, distinct, from, joins, where, List.of(), null, List.of(exprs));
        }

        public OrderByStage orderBy(String... columns) {
            return orderBy(toColumnRefs(columns));
        }

        public LimitStage limit(int limit) {
            return new LimitStage(projections, distinct, from, joins, where, List.of(), null, List.of(), new Limit(limit));
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, joins, where, List.of(), null, List.of(), null, null, Optional.empty()).build(outer);
        }
    }

    public final class GroupByStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;
        private final List<JoinEntry>  joins;
        private final Expr             where;
        private final List<Expr>       groupBy;

        GroupByStage(List<Projection> projections, boolean distinct, FromSource from,
                     List<JoinEntry> joins, Expr where, List<Expr> groupBy) {
            
            if (groupBy.isEmpty()) throw new IllegalArgumentException("GROUP BY requires at least one expression");
            for (var e : groupBy) requireNonNull(e, "GROUP BY expression cannot be null");
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
            this.where       = where;
            this.groupBy     = groupBy;
        }

        public HavingStage having(Expr condition) {
            return new HavingStage(projections, distinct, from, joins, where, groupBy,
                requireNonNull(condition, "HAVING condition cannot be null"));
        }

        public OrderByStage orderBy(Expr... exprs) {
            return new OrderByStage(projections, distinct, from, joins, where, groupBy, null, List.of(exprs));
        }

        public OrderByStage orderBy(String... columns) {
            return orderBy(toColumnRefs(columns));
        }

        public LimitStage limit(int limit) {
            return new LimitStage(projections, distinct, from, joins, where, groupBy, null, List.of(), new Limit(limit));
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, null, List.of(), null, null, Optional.empty()).build(outer);
        }
    }

    public final class HavingStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;
        private final List<JoinEntry>  joins;
        private final Expr             where;
        private final List<Expr>       groupBy;
        private final Expr             having;

        HavingStage(List<Projection> projections, boolean distinct, FromSource from,
                    List<JoinEntry> joins, Expr where, List<Expr> groupBy, Expr having) {
            
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
            this.where       = where;
            this.groupBy     = groupBy;
            this.having      = having;
        }

        public OrderByStage orderBy(Expr... exprs) {
            return new OrderByStage(projections, distinct, from, joins, where, groupBy, having, List.of(exprs));
        }

        public OrderByStage orderBy(String... columns) {
            return orderBy(toColumnRefs(columns));
        }

        public LimitStage limit(int limit) {
            return new LimitStage(projections, distinct, from, joins, where, groupBy, having, List.of(), new Limit(limit));
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, List.of(), null, null, Optional.empty()).build(outer);
        }
    }

    public final class OrderByStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;
        private final List<JoinEntry>  joins;
        private final Expr             where;
        private final List<Expr>       groupBy;
        private final Expr             having;
        private final List<Expr>       orderBy;

        OrderByStage(List<Projection> projections, boolean distinct, FromSource from,
                     List<JoinEntry> joins, Expr where, List<Expr> groupBy, Expr having, List<Expr> orderBy) {
            
            if (orderBy.isEmpty()) throw new IllegalArgumentException("ORDER BY requires at least one expression");
            for (var e : orderBy) requireNonNull(e, "ORDER BY expression cannot be null");
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
            this.where       = where;
            this.groupBy     = groupBy;
            this.having      = having;
            this.orderBy     = orderBy;
        }

        public LimitStage limit(int limit) {
            return new LimitStage(projections, distinct, from, joins, where, groupBy, having, orderBy, new Limit(limit));
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, null, null, Optional.empty()).build(outer);
        }
    }

    public final class LimitStage {
        private final List<Projection> projections;
        private final boolean          distinct;
        private final FromSource       from;
        private final List<JoinEntry>  joins;
        private final Expr             where;
        private final List<Expr>       groupBy;
        private final Expr             having;
        private final List<Expr>       orderBy;
        private final Limit            limit;

        LimitStage(List<Projection> projections, boolean distinct, FromSource from,
                   List<JoinEntry> joins, Expr where, List<Expr> groupBy, Expr having,
                   List<Expr> orderBy, Limit limit) {
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
            this.where       = where;
            this.groupBy     = groupBy;
            this.having      = having;
            this.orderBy     = orderBy;
            this.limit       = requireNonNull(limit, "Limit cannot be null");
        }

        public BuildStage offset(int offset) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, new Offset(offset), Optional.empty());
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null, 
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null, Optional.empty()).build();
        }

        public JQ.Read build(JQ.Read outer) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, null, Optional.empty()).build(outer);
        }
    }

    public final class BuildStage {
        private final List<Projection>    projections;
        private final boolean             distinct;
        private final FromSource          from;
        private final List<JoinEntry>     joins;
        private final Expr                where;
        private final List<Expr>          groupBy;
        private final Expr                having;
        private final List<Expr>          orderBy;
        private final Limit               limit;
        private final Offset              offset;
        private final Optional<ForUpdate> forUpdate;

        BuildStage(List<Projection> projections, boolean distinct, FromSource from,
                   List<JoinEntry> joins, Expr where, List<Expr> groupBy, Expr having,
                   List<Expr> orderBy, Limit limit, Offset offset, Optional<ForUpdate> forUpdate) {
            
            this.projections = projections;
            this.distinct    = distinct;
            this.from        = from;
            this.joins       = joins;
            this.where       = where;
            this.groupBy     = groupBy;
            this.having      = having;
            this.orderBy     = orderBy;
            this.limit       = limit;
            this.offset      = offset;
            this.forUpdate   = forUpdate;
        }

        public BuildStage forUpdate() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset,
                Optional.of(ForUpdate.simple()));
        }

        public BuildStage forUpdateNoWait() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset,
                Optional.of(ForUpdate.noWait()));
        }

        public BuildStage forUpdateSkipLocked() {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset,
                Optional.of(ForUpdate.skipLocked()));
        }

        public BuildStage forUpdateOf(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset,
                Optional.of(ForUpdate.of(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfNoWait(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset,
                Optional.of(ForUpdate.ofNoWait(java.util.List.of(columns))));
        }

        public BuildStage forUpdateOfSkipLocked(String... columns) {
            return new BuildStage(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset,
                Optional.of(ForUpdate.ofSkipLocked(java.util.List.of(columns))));
        }

        public JQ.Read build() {
            return new JQ.Read(buildSql(), buildContext(Optional.empty()));
        }

        public JQ.Read build(JQ.Read outer) {
            return new JQ.Read(buildSql(), buildContext(Optional.of((Context) outer.context())));
        }

        private String buildSql() {
            return SelectSQL.build(
                projections,
                distinct,
                from,
                joins,
                Optional.ofNullable(where),
                groupBy,
                Optional.ofNullable(having),
                orderBy,
                Optional.ofNullable(limit),
                Optional.ofNullable(offset),
                forUpdate
            );
        }

        private Context.Select buildContext(Optional<Context> outer) {
            var allSources = new ArrayList<TableSource>();
            allSources.add(from.toTableSource());
            joins.forEach(j -> allSources.add(j.source().toTableSource()));

            var refs = projections.stream()
                .map(p -> (Ref) new Ref.Named(p))
                .toList();
                
            var joinsExpressions = joins.stream()
                .filter(j -> j instanceof JoinedOn)
                .map(j -> ((JoinedOn) j).on())
                .toList();

            return ContextFactory.selectContext(
                allSources,
                refs,
                joinsExpressions,
                Optional.ofNullable(where),
                groupBy,
                Optional.ofNullable(having),
                orderBy,
                outer
            );
        }
    }

    private static Projection[] toProjections(Expr[] exprs) {
        var result = new Projection[exprs.length];
        for (var i = 0; i < exprs.length; i++) result[i] = new Projection.Base(exprs[i]);
        return result;
    }

    private static Expr[] toColumnRefs(String[] columns) {
        return Arrays.stream(columns)
            .map(ColumnRef.Base::new)
            .toArray(Expr[]::new);
    }

    private static Projection[] toQW(String... qualifiedWildcards) {
        return Arrays.stream(qualifiedWildcards).map(Projection.QualifiedWildcard::new).toArray(Projection[]::new);
    }
}