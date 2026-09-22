package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.Criteria;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.value_objects.Projection;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class CriteriaBuilder {

    private CriteriaBuilder() {}

    public static SelectStages.SelectStage<Criteria> select(Expr... exprs) {
        requireNonNull(exprs, "Expressions cannot be null");
        return start(false, toProjections(exprs));
    }

    public static SelectStages.SelectStage<Criteria> selectDistinct(Expr... exprs) {
        requireNonNull(exprs, "Expressions cannot be null");
        return start(true, toProjections(exprs));
    }

    public static SelectStages.SelectStage<Criteria> select(Projection... projections) {
        return start(false, requireNonNull(projections, "Projections cannot be null"));
    }

    public static SelectStages.SelectStage<Criteria> selectDistinct(Projection... projections) {
        return start(true, requireNonNull(projections, "Projections cannot be null"));
    }

    public static SelectStages.SelectStage<Criteria> selectAll() {
        return start(false, new Projection.Wildcard());
    }

    public static SelectStages.SelectStage<Criteria> selectAllDistinct() {
        return start(true, new Projection.Wildcard());
    }

    private static SelectStages.SelectStage<Criteria> start(boolean distinct, Projection... projections) {
        if (projections.length == 0) throw new IllegalArgumentException("At least one projection is required");
        for (var p : projections) requireNonNull(p, "Projection cannot be null");
        return SelectStages.select(List.of(projections), distinct, CriteriaBuilder::finish);
    }

    private static Criteria finish(QuerySpec spec) {
        return new Criteria(ContextFactory.criteriaContext(
            spec.projections(),
            spec.distinct(),
            spec.from(),
            spec.joins(),
            Optional.ofNullable(spec.where()),
            spec.groupBy(),
            Optional.ofNullable(spec.having()),
            spec.orderBy(),
            Optional.ofNullable(spec.limit()),
            Optional.ofNullable(spec.offset()),
            spec.forUpdate(),
            Optional.empty()
        ));
    }

    private static Projection[] toProjections(Expr[] exprs) {
        var result = new Projection[exprs.length];
        for (var i = 0; i < exprs.length; i++) result[i] = new Projection.Base(exprs[i]);
        return result;
    }
}