package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.internal.value_objects.ForUpdate;
import io.github.hacihaciyev.sql.internal.value_objects.FromSource;
import io.github.hacihaciyev.sql.internal.value_objects.JoinEntry;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.Projection;

import java.util.List;
import java.util.Optional;

record QuerySpec(
    List<Projection> projections,
    boolean distinct,
    FromSource from,
    List<JoinEntry> joins,
    Expr where,
    List<Expr> groupBy,
    Expr having,
    List<Expr> orderBy,
    Limit limit,
    Offset offset,
    Optional<ForUpdate> forUpdate
) {
    QuerySpec withJoins(List<JoinEntry> joins) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withWhere(Expr where) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withGroupBy(List<Expr> groupBy) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withHaving(Expr having) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withOrderBy(List<Expr> orderBy) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withLimit(Limit limit) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withOffset(Offset offset) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }

    QuerySpec withForUpdate(Optional<ForUpdate> forUpdate) {
        return new QuerySpec(projections, distinct, from, joins, where, groupBy, having, orderBy, limit, offset, forUpdate);
    }
}