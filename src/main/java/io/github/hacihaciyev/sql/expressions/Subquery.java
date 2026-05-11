package io.github.hacihaciyev.sql.expressions;

import io.github.hacihaciyev.sql.JQ;
import static java.util.Objects.requireNonNull;

public sealed interface Subquery {
    
    record Scalar(JQ.Read jq) implements Subquery, ValueExpr {
        public Scalar {
            requireNonNull(jq);
        }
    }
    
    record Table(JQ.Read jq) implements Subquery {
        public Table {
            requireNonNull(jq);
        }
    }
}