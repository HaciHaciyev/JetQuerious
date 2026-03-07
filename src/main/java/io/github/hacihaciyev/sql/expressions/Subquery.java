package io.github.hacihaciyev.sql.expressions;

import io.github.hacihaciyev.sql.JQ;
import static java.util.Objects.requireNonNull;

public sealed interface Subquery extends Expr {
    
    record ScalarSubquery(JQ jq) implements Subquery {
        public ScalarSubquery {
            requireNonNull(jq);
        }
    }
    
    record TableSubquery(JQ jq) implements Subquery {
        public TableSubquery {
            requireNonNull(jq);
        }
    }
}