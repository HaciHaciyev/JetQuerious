package io.github.hacihaciyev.sql.expressions;

import java.util.List;

import static java.util.Objects.requireNonNull;

public sealed interface CaseExpr extends Expr {

    record WhenThen(Expr condition, Expr result) {
        public WhenThen {
            requireNonNull(condition);
            requireNonNull(result);
        }
    }

    record SimpleCase(Expr operand, List<WhenThen> branches) implements CaseExpr {
        public SimpleCase {
            requireNonNull(operand);
            requireNonNull(branches);
            if (branches.isEmpty()) throw new IllegalArgumentException("CASE requires at least one branch");

            branches = List.copyOf(branches);
            for (var b : branches) requireNonNull(b);
        }
    }
    
    record Case(List<WhenThen> branches) implements CaseExpr {
        public Case {
            requireNonNull(branches);
            if (branches.isEmpty()) throw new IllegalArgumentException("CASE requires at least one branch");
             
            branches = List.copyOf(branches);
            for (var b : branches) requireNonNull(b);
        }
    }

    record CaseElse(List<WhenThen> branches, Expr elseBranch) implements CaseExpr {
        public CaseElse {
            requireNonNull(branches);
            requireNonNull(elseBranch);
            if (branches.isEmpty()) throw new IllegalArgumentException("CASE requires at least one branch");
             
            branches = List.copyOf(branches);
            for (var b : branches) requireNonNull(b);
        }
    }

    record SimpleCaseElse(Expr operand, List<WhenThen> branches, Expr elseBranch) implements CaseExpr {
        public SimpleCaseElse {
            requireNonNull(operand);
            requireNonNull(branches);
            requireNonNull(elseBranch);
            if (branches.isEmpty()) throw new IllegalArgumentException("CASE requires at least one branch");
            branches = List.copyOf(branches);
            for (var b : branches) requireNonNull(b);
        }
    }
}