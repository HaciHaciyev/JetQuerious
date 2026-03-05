package io.github.hacihaciyev.sql.expressions;

import java.util.List;
import io.github.hacihaciyev.types.SQLType;
import static java.util.Objects.requireNonNull;

public sealed interface Func extends Expr {

    sealed interface Aggregate permits Count, CountAll, Sum, Avg, Min, Max {}
    sealed interface Text permits Upper, Lower, Trim, Substring, Length {}
    sealed interface Temporal permits CurrentDate, CurrentTimestamp, Extract {}
    sealed interface Conditional permits Coalesce, NullIf {}
    sealed interface Numeric permits Abs, Round, Floor, Ceil, Power, Sqrt, Mod {}
    sealed interface Conversion permits Cast {}

    record Count(Expr expr, boolean distinct) implements Func, Aggregate {
        public Count {
            requireNonNull(expr);
        }
    }

    record CountAll(boolean distinct) implements Func, Aggregate {}

    record Sum(Expr expr, boolean distinct) implements Func, Aggregate {
        public Sum {
            requireNonNull(expr);
        }
    }

    record Avg(Expr expr, boolean distinct) implements Func, Aggregate {
        public Avg {
            requireNonNull(expr);
        }
    }

    record Min(Expr expr) implements Func, Aggregate {
        public Min {
            requireNonNull(expr);
        }
    }

    record Max(Expr expr) implements Func, Aggregate {
        public Max {
            requireNonNull(expr);
        }
    }

    record Upper(Expr value) implements Func, Text {
        public Upper {
            requireNonNull(value);
        }
    }

    record Lower(Expr value) implements Func, Text {
        public Lower {
            requireNonNull(value);
        }
    }

    record Trim(Expr value) implements Func, Text {
        public Trim {
            requireNonNull(value);
        }
    }

    record Length(Expr value) implements Func, Text {
        public Length {
            requireNonNull(value);
        }
    }

    record Substring(Expr value, Expr start, Expr length) implements Func, Text {
        public Substring {
            requireNonNull(value);
            requireNonNull(start);
            requireNonNull(length);
        }
    }

    record CurrentDate() implements Func, Temporal {}

    record CurrentTimestamp() implements Func, Temporal {}

    record Extract(TemporalField field, Expr source) implements Func, Temporal {
        public Extract {
            requireNonNull(field);
            requireNonNull(source);
        }
    }

    enum TemporalField {
        YEAR, MONTH, DAY, HOUR, MINUTE, SECOND
    }

    record Coalesce(List<Expr> values) implements Func, Conditional {
        public Coalesce {
            requireNonNull(values);
            if (values.size() < 2) throw new IllegalArgumentException("COALESCE requires at least 2 values");
            
            values = List.copyOf(values);
            for (var v : values) requireNonNull(v);
        }
    }

    record NullIf(Expr first, Expr second) implements Func, Conditional {
        public NullIf {
            requireNonNull(first);
            requireNonNull(second);
        }
    }

    record Abs(Expr value) implements Func, Numeric {
        public Abs {
            requireNonNull(value);
        }
    }

    record Round(Expr value, Expr precision) implements Func, Numeric {
        public Round {
            requireNonNull(value);
            requireNonNull(precision);
        }
    }

    record Floor(Expr value) implements Func, Numeric {
        public Floor {
            requireNonNull(value);
        }
    }

    record Ceil(Expr value) implements Func, Numeric {
        public Ceil {
            requireNonNull(value);
        }
    }

    record Power(Expr base, Expr exponent) implements Func, Numeric {
        public Power {
            requireNonNull(base);
            requireNonNull(exponent);
        }
    }

    record Sqrt(Expr value) implements Func, Numeric {
        public Sqrt {
            requireNonNull(value);
        }
    }

    record Mod(Expr left, Expr right) implements Func, Numeric {
        public Mod {
            requireNonNull(left);
            requireNonNull(right);
        }
    }

    record Cast(Expr value, SQLType targetType) implements Func, Conversion {
        public Cast {
            requireNonNull(value);
            requireNonNull(targetType);
        }
    }
}