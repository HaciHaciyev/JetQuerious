package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.expressions.BinaryOp.BinaryOperator;
import io.github.hacihaciyev.sql.expressions.CaseExpr.WhenThen;
import io.github.hacihaciyev.sql.expressions.Func.TemporalField;
import io.github.hacihaciyev.sql.expressions.QuantifiedExpr.ComparisonOperator;
import io.github.hacihaciyev.sql.expressions.UnaryOp.UnaryOperator;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.types.SQLType;

import java.math.BigDecimal;
import java.util.List;

public final class SQL {

    private SQL() {}

    public static ColumnRef.Base col(String name) {
        return new ColumnRef.Base(name);
    }

    public static ColumnRef.Base col(String name, Class<?> type) {
        return new ColumnRef.Base(name, new ColumnRef.Type.Some(type));
    }

    public static ColumnRef.VariableBase col(String table, String name) {
        return new ColumnRef.VariableBase(table, name);
    }

    public static ColumnRef.VariableBase col(String table, String name, Class<?> type) {
        return new ColumnRef.VariableBase(table, name, new ColumnRef.Type.Some(type));
    }

    public static ColumnRef.Alias colAs(String name, String alias) {
        return new ColumnRef.Alias(name, alias);
    }

    public static ColumnRef.Alias colAs(String name, String alias, Class<?> type) {
        return new ColumnRef.Alias(name, alias, new ColumnRef.Type.Some(type));
    }

    public static ColumnRef.VariableAlias colAs(String table, String name, String alias) {
        return new ColumnRef.VariableAlias(table, name, alias);
    }

    public static ColumnRef.VariableAlias colAs(String table, String name, String alias, Class<?> type) {
        return new ColumnRef.VariableAlias(table, name, alias, new ColumnRef.Type.Some(type));
    }
    
    public static Literal.PlaceholderLiteral param(Class<?> type) {
        return new Literal.PlaceholderLiteral(type);
    }

    public static Literal.StringLiteral lit(String value) {
        return new Literal.StringLiteral(value);
    }

    public static Literal.IntLiteral lit(int value) {
        return new Literal.IntLiteral(value);
    }

    public static Literal.LongLiteral lit(long value) {
        return new Literal.LongLiteral(value);
    }

    public static Literal.BooleanLiteral lit(boolean value) {
        return new Literal.BooleanLiteral(value);
    }

    public static Literal.FloatLiteral lit(float value) {
        return new Literal.FloatLiteral(value);
    }

    public static Literal.DoubleLiteral lit(double value) {
        return new Literal.DoubleLiteral(value);
    }

    public static Literal.BigDecimalLiteral lit(BigDecimal value) {
        return new Literal.BigDecimalLiteral(value);
    }

    public static Literal.NullLiteral litNull() {
        return new Literal.NullLiteral();
    }

    public static BinaryOp eq(Expr left, Expr right) {
        return bin(BinaryOperator.EQ, left, right);
    }

    public static BinaryOp neq(Expr left, Expr right) {
        return bin(BinaryOperator.NEQ, left, right);
    }

    public static BinaryOp gt(Expr left, Expr right) {
        return bin(BinaryOperator.GT, left, right);
    }

    public static BinaryOp gte(Expr left, Expr right) {
        return bin(BinaryOperator.GTE, left, right);
    }

    public static BinaryOp lt(Expr left, Expr right) {
        return bin(BinaryOperator.LT, left, right);
    }

    public static BinaryOp lte(Expr left, Expr right) {
        return bin(BinaryOperator.LTE, left, right);
    }

    public static BinaryOp like(Expr left, Expr right) {
        return bin(BinaryOperator.LIKE, left, right);
    }

    public static BinaryOp notLike(Expr left, Expr right) {
        return bin(BinaryOperator.NOT_LIKE, left, right);
    }

    public static BinaryOp and(Expr left, Expr right) {
        return bin(BinaryOperator.AND, left, right);
    }

    public static BinaryOp or(Expr left, Expr right) {
        return bin(BinaryOperator.OR, left, right);
    }

    public static UnaryOp not(Expr expr) {
        return new UnaryOp(UnaryOperator.NOT, expr);
    }

    public static BinaryOp and(Expr first, Expr second, Expr... rest) {
        BinaryOp result = and(first, second);
        for (Expr expr : rest) result = and(result, expr);
        return result;
    }

    public static BinaryOp or(Expr first, Expr second, Expr... rest) {
        BinaryOp result = or(first, second);
        for (Expr expr : rest) result = or(result, expr);
        return result;
    }

    public static BinaryOp add(Expr left, Expr right) {
        return bin(BinaryOperator.PLUS, left, right);
    }

    public static BinaryOp subtract(Expr left, Expr right) {
        return bin(BinaryOperator.MINUS, left, right);
    }

    public static BinaryOp multiply(Expr left, Expr right) {
        return bin(BinaryOperator.MULTIPLY, left, right);
    }

    public static BinaryOp divide(Expr left, Expr right) {
        return bin(BinaryOperator.DIV, left, right);
    }

    public static UnaryOp negate(Expr expr) {
        return new UnaryOp(UnaryOperator.MINUS, expr);
    }

    public static IsNullExpr.IsNull isNull(Expr operand) {
        return new IsNullExpr.IsNull(operand);
    }

    public static IsNullExpr.IsNotNull isNotNull(Expr operand) {
        return new IsNullExpr.IsNotNull(operand);
    }

    public static BetweenExpr.Between between(Expr operand, Expr low, Expr high) {
        return new BetweenExpr.Between(operand, low, high);
    }

    public static BetweenExpr.NotBetween notBetween(Expr operand, Expr low, Expr high) {
        return new BetweenExpr.NotBetween(operand, low, high);
    }

    public static InExpr.In in(Expr operand, Expr... values) {
        return new InExpr.In(
                operand,
                new InExpr.ValueList(List.of(values))
        );
    }

    public static InExpr.NotIn notIn(Expr operand, Expr... values) {
        return new InExpr.NotIn(
                operand,
                new InExpr.ValueList(List.of(values))
        );
    }

    public static InExpr.In in(Expr operand, Subquery.Table subquery) {
        return new InExpr.In(
                operand,
                new InExpr.SubquerySource(subquery)
        );
    }

    public static InExpr.NotIn notIn(Expr operand, Subquery.Table subquery) {
        return new InExpr.NotIn(
                operand,
                new InExpr.SubquerySource(subquery)
        );
    }

    public static Exists exists(Subquery.Table subquery) {
        return new Exists(subquery);
    }

    public static Subquery.Scalar scalar(JQ.Read jq) {
        return new Subquery.Scalar(jq);
    }

    public static Subquery.Table table(JQ.Read jq) {
        return new Subquery.Table(jq);
    }

    public static Func.CountAll countAll() {
        return new Func.CountAll();
    }

    public static Func.Count count(Expr expr) {
        return new Func.Count(expr, false);
    }

    public static Func.Count countDistinct(Expr expr) {
        return new Func.Count(expr, true);
    }

    public static Func.Sum sum(Expr expr) {
        return new Func.Sum(expr, false);
    }

    public static Func.Sum sumDistinct(Expr expr) {
        return new Func.Sum(expr, true);
    }

    public static Func.Avg avg(Expr expr) {
        return new Func.Avg(expr, false);
    }

    public static Func.Avg avgDistinct(Expr expr) {
        return new Func.Avg(expr, true);
    }

    public static Func.Min min(Expr expr) {
        return new Func.Min(expr);
    }

    public static Func.Max max(Expr expr) {
        return new Func.Max(expr);
    }

    public static Func.Upper upper(Expr expr) {
        return new Func.Upper(expr);
    }

    public static Func.Lower lower(Expr expr) {
        return new Func.Lower(expr);
    }

    public static Func.Trim trim(Expr expr) {
        return new Func.Trim(expr);
    }

    public static Func.Length length(Expr expr) {
        return new Func.Length(expr);
    }

    public static Func.Substring substring(Expr value, Expr start, Expr len) {
        return new Func.Substring(value, start, len);
    }

    public static Func.Abs abs(Expr expr) {
        return new Func.Abs(expr);
    }

    public static Func.Round round(Expr value, Expr prec) {
        return new Func.Round(value, prec);
    }

    public static Func.Floor floor(Expr expr) {
        return new Func.Floor(expr);
    }

    public static Func.Ceil ceil(Expr expr) {
        return new Func.Ceil(expr);
    }

    public static Func.Power power(Expr base, Expr exp) {
        return new Func.Power(base, exp);
    }

    public static Func.Sqrt sqrt(Expr expr) {
        return new Func.Sqrt(expr);
    }

    public static Func.Mod mod(Expr left, Expr right) {
        return new Func.Mod(left, right);
    }

    public static Func.CurrentDate currentDate() {
        return new Func.CurrentDate();
    }

    public static Func.CurrentTimestamp currentTimestamp() {
        return new Func.CurrentTimestamp();
    }

    public static Func.Extract extract(TemporalField field, Expr src) {
        return new Func.Extract(field, src);
    }

    public static Func.Coalesce coalesce(Expr... values) {
        return new Func.Coalesce(List.of(values));
    }

    public static Func.NullIf nullIf(Expr first, Expr second) {
        return new Func.NullIf(first, second);
    }

    public static Func.Cast cast(Expr value, SQLType type) {
        return new Func.Cast(value, type);
    }

    public static CaseExpr.Case caseWhen(WhenThen... branches) {
        return new CaseExpr.Case(List.of(branches));
    }

    public static CaseExpr.CaseElse caseWhen(
            Expr elseBranch,
            WhenThen... branches
    ) {
        return new CaseExpr.CaseElse(
                List.of(branches),
                elseBranch
        );
    }

    public static CaseExpr.SimpleCase caseOf(
            Expr operand,
            WhenThen... branches
    ) {
        return new CaseExpr.SimpleCase(
                operand,
                List.of(branches)
        );
    }

    public static CaseExpr.SimpleCaseElse caseOf(
            Expr operand,
            Expr elseBranch,
            WhenThen... branches
    ) {
        return new CaseExpr.SimpleCaseElse(
                operand,
                List.of(branches),
                elseBranch
        );
    }

    public static WhenThen when(Expr condition, Expr result) {
        return new CaseExpr.WhenThen(condition, result);
    }

    public static QuantifiedExpr.All all(
            ComparisonOperator op,
            Expr operand,
            Subquery.Table sub
    ) {
        return new QuantifiedExpr.All(op, operand, sub);
    }

    public static QuantifiedExpr.Any any(
            ComparisonOperator op,
            Expr operand,
            Subquery.Table sub
    ) {
        return new QuantifiedExpr.Any(op, operand, sub);
    }

    private static BinaryOp bin(
            BinaryOperator op,
            Expr left,
            Expr right
    ) {
        return new BinaryOp(op, left, right);
    }
    
    public static TableRef.Base t(String name) {
           return new TableRef.Base(name);
    }
    
    public static TableRef.WithSchema t(String schema, String name) {
        return new TableRef.WithSchema(schema, name);
    }
    
    public static TableRef.WithCatalogAndSchema t(String catalog, String schema, String name) {
        return new TableRef.WithCatalogAndSchema(catalog, schema, name);
    }
    
    public static TableRef.AliasedBase tAs(String name, String alias) {
        return new TableRef.AliasedBase(name, alias);
    }
    
    public static TableRef.AliasedWithSchema tAs(String schema, String name, String alias) {
        return new TableRef.AliasedWithSchema(schema, name, alias);
    }
    
    public static TableRef.AliasedWithCatalogAndSchema tAs(String catalog, String schema, String name, String alias) {
        return new TableRef.AliasedWithCatalogAndSchema(catalog, schema, name, alias);
    }
}