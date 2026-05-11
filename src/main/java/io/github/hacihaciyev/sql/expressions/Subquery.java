package io.github.hacihaciyev.sql.expressions;

import io.github.hacihaciyev.sql.JQ;
import static java.util.Objects.requireNonNull;

public sealed interface Subquery {
    
    record ScalarSubquery(JQ.Read jq) implements Subquery, ValueExpr {
        public ScalarSubquery {
            requireNonNull(jq);
        }
    }
    
    record TableSubquery(JQ.Read jq) implements Subquery {
        public TableSubquery {
            requireNonNull(jq);
        }
    }
}