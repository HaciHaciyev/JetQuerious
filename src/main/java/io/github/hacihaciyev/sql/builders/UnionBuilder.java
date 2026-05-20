package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.internal.builders.UnionSQL;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.UnionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class UnionBuilder {
    private final UnionType     unionType;
    private final List<JQ.Read> queries;

    public UnionBuilder(UnionType unionType, JQ.Read first, JQ.Read... rest) {
        this.unionType = requireNonNull(unionType, "Union type cannot be null");
        requireNonNull(first, "First query cannot be null");
        requireNonNull(rest,  "Rest queries cannot be null");

        var queries = new ArrayList<JQ.Read>();
        queries.add(first);
        for (var q : rest) queries.add(requireNonNull(q, "Query cannot be null"));
        this.queries = List.copyOf(queries);
    }

    public UnionBuilder add(JQ.Read query) {
        requireNonNull(query, "Query cannot be null");
        var queries = new ArrayList<>(this.queries);
        queries.add(query);
        return new UnionBuilder(unionType, queries.getFirst(), queries.subList(1, queries.size()).toArray(JQ.Read[]::new));
    }

    public OrderByStage orderBy(Expr... exprs) {
        if (exprs.length == 0) throw new IllegalArgumentException("At least one ORDER BY expression is required");
        for (var e : exprs) requireNonNull(e, "ORDER BY expression cannot be null");
        return new OrderByStage(List.of(exprs));
    }

    public OrderByStage orderBy(String... columns) {
        return orderBy(Arrays.stream(columns)
            .map(ColumnRef.Base::new)
            .toArray(Expr[]::new));
    }

    public LimitStage limit(int value) {
        return new LimitStage(List.of(), new Limit(value));
    }

    public JQ.Read build() {
        return new BuildStage(List.of(), null, null).build();
    }

    public final class OrderByStage {
        private final List<Expr> orderBy;

        OrderByStage(List<Expr> orderBy) {
            this.orderBy = orderBy;
        }

        public LimitStage limit(int value) {
            return new LimitStage(orderBy, new Limit(value));
        }

        public JQ.Read build() {
            return new BuildStage(orderBy, null, null).build();
        }
    }

    public final class LimitStage {
        private final List<Expr> orderBy;
        private final Limit      limit;

        LimitStage(List<Expr> orderBy, Limit limit) {
            this.orderBy = orderBy;
            this.limit   = limit;
        }

        public JQ.Read offset(int value) {
            return new BuildStage(orderBy, limit, new Offset(value)).build();
        }

        public JQ.Read build() {
            return new BuildStage(orderBy, limit, null).build();
        }
    }

    private final class BuildStage {
        private final List<Expr> orderBy;
        private final Limit      limit;
        private final Offset     offset;

        BuildStage(List<Expr> orderBy, Limit limit, Offset offset) {
            this.orderBy = orderBy;
            this.limit   = limit;
            this.offset  = offset;
        }

        public JQ.Read build() {
            var sqls = queries.stream().map(JQ.Read::sql).toList();
            var sql  = UnionSQL.build(
                unionType,
                sqls,
                orderBy,
                Optional.ofNullable(limit),
                Optional.ofNullable(offset)
            );
            return new JQ.Read(sql, queries.get(0).context());
        }
    }
}