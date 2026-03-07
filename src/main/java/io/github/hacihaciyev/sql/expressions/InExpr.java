package io.github.hacihaciyev.sql.expressions;

import java.util.List;

import static java.util.Objects.requireNonNull;

public sealed interface InExpr extends ValueExpr {

    Expr operand();

    sealed interface InSource permits ValueList, SubquerySource {}

    record ValueList(List<Expr> values) implements InSource {
        public ValueList {
            requireNonNull(values);
            if (values.isEmpty()) throw new IllegalArgumentException("IN requires at least one value");
            values = List.copyOf(values);
            for (var v : values) requireNonNull(v);
        }
    }

    record SubquerySource(Subquery.TableSubquery subquery) implements InSource {
        public SubquerySource {
            requireNonNull(subquery);
        }
    }

    record In(Expr operand, InSource source) implements InExpr {
        public In {
            requireNonNull(operand);
            requireNonNull(source);
        }
    }

    record NotIn(Expr operand, InSource source) implements InExpr {
        public NotIn {
            requireNonNull(operand);
            requireNonNull(source);
        }
    }
}