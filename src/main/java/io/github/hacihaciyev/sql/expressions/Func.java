package io.github.hacihaciyev.sql.expressions;

import java.util.List;
import java.util.stream.Collectors;

import io.github.hacihaciyev.types.SQLType;

public sealed interface Func extends Expr {

    sealed interface Aggregate permits Count, Sum, Avg, Min, Max {}
    sealed interface Text permits Upper, Lower, Trim, Substring, Length {}
    sealed interface Temporal permits CurrentDate, CurrentTimestamp, Extract {}
    sealed interface Conditional permits Case, Coalesce, NullIf {}
    sealed interface Numeric permits Abs, Round, Floor, Ceil, Power, Sqrt, Mod {}
    sealed interface Conversion permits Cast {}

    public record Count(Expr expr, boolean distinct) implements Func, Aggregate {
        @Override
        public String toString() {
            if (expr == null) return "COUNT(*)";
            return "COUNT(" + (distinct ? "DISTINCT " : "") + expr + ")";
        }
    }

    public record Sum(Expr expr, boolean distinct) implements Func, Aggregate {
        @Override
        public String toString() {
            return "SUM(" + (distinct ? "DISTINCT " : "") + expr + ")";
        }
    }

    public record Avg(Expr expr, boolean distinct) implements Func, Aggregate {
        @Override
        public String toString() {
            return "AVG(" + (distinct ? "DISTINCT " : "") + expr + ")";
        }
    }

    public record Min(Expr expr) implements Func, Aggregate {
        @Override
        public String toString() {
            return "MIN(" + expr + ")";
        }
    }

    public record Max(Expr expr) implements Func, Aggregate {
        @Override
        public String toString() {
            return "MAX(" + expr + ")";
        }
    }

    public record Upper(Expr value) implements Func, Text {
        @Override
        public String toString() {
            return "UPPER(" + value + ")";
        }
    }

    public record Lower(Expr value) implements Func, Text {
        @Override
        public String toString() {
            return "LOWER(" + value + ")";
        }
    }

    public record Trim(Expr value) implements Func, Text {
        @Override
        public String toString() {
            return "TRIM(" + value + ")";
        }
    }

    public record Length(Expr value) implements Func, Text {
        @Override
        public String toString() {
            return "LENGTH(" + value + ")";
        }
    }

    public record Substring(Expr value, Expr start, Expr length) implements Func, Text {
        @Override
        public String toString() {
            return "SUBSTRING(" + value + ", " + start + ", " + length + ")";
        }
    }

    public record CurrentDate() implements Func, Temporal {
        @Override
        public String toString() {
            return "CURRENT_DATE";
        }
    }

    public record CurrentTimestamp() implements Func, Temporal {
        @Override
        public String toString() {
            return "CURRENT_TIMESTAMP";
        }
    }

    public record Extract(TemporalField field, Expr source) implements Func, Temporal {
        @Override
        public String toString() {
            return "EXTRACT(" + field.name() + " FROM " + source + ")";
        }
    }

    enum TemporalField {
        YEAR, MONTH, DAY, HOUR, MINUTE, SECOND
    }

    public record Coalesce(List<Expr> values) implements Func, Conditional {
        @Override
        public String toString() {
            var joined = values.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "COALESCE(" + joined + ")";
        }
    }

    public record NullIf(Expr first, Expr second) implements Func, Conditional {
        @Override
        public String toString() {
            return "NULLIF(" + first + ", " + second + ")";
        }
    }

    public record Case(List<WhenThen> branches, Expr elseExpr) implements Func, Conditional {
        @Override
        public String toString() {
            var sb = new StringBuilder("CASE ");
            for (var branch : branches) {
                sb.append("WHEN ")
                  .append(branch.condition())
                  .append(" THEN ")
                  .append(branch.result())
                  .append(" ");
            }
            if (elseExpr != null) {
                sb.append("ELSE ").append(elseExpr).append(" ");
            }
            sb.append("END");
            return sb.toString();
        }
    }

    public record WhenThen(Expr condition, Expr result) {}

    public record Abs(Expr value) implements Func, Numeric {
        @Override
        public String toString() {
            return "ABS(" + value + ")";
        }
    }

    public record Round(Expr value, Expr precision) implements Func, Numeric {
        @Override
        public String toString() {
            return "ROUND(" + value + ", " + precision + ")";
        }
    }

    public record Floor(Expr value) implements Func, Numeric {
        @Override
        public String toString() {
            return "FLOOR(" + value + ")";
        }
    }

    public record Ceil(Expr value) implements Func, Numeric {
        @Override
        public String toString() {
            return "CEIL(" + value + ")";
        }
    }

    public record Power(Expr base, Expr exponent) implements Func, Numeric {
        @Override
        public String toString() {
            return "POWER(" + base + ", " + exponent + ")";
        }
    }

    public record Sqrt(Expr value) implements Func, Numeric {
        @Override
        public String toString() {
            return "SQRT(" + value + ")";
        }
    }

    public record Mod(Expr left, Expr right) implements Func, Numeric {
        @Override
        public String toString() {
            return "MOD(" + left + ", " + right + ")";
        }
    }

    public record Cast(Expr value, SQLType targetType) implements Func, Conversion {
        @Override
        public String toString() {
            return "CAST(" + value + " AS " + targetType + ")";
        }
    }
}