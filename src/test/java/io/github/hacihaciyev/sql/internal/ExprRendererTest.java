package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.expressions.CaseExpr.WhenThen;
import io.github.hacihaciyev.sql.internal.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExprRendererTest {

    private static final ColumnRef.Base COL_A = new ColumnRef.Base("a");
    private static final ColumnRef.Base COL_B = new ColumnRef.Base("b");
    private static final ColumnRef.Base COL_C = new ColumnRef.Base("c");
    private static final Literal.IntLiteral ONE = new Literal.IntLiteral(1);

    private static Subquery.Table tableSubquery(String sql) {
        return new Subquery.Table(TestFixtures.stubJqRead());
    }

    // ── ColumnRef ─────────────────────────────────────────────────────────────

    @Test
    void columnRef_base() {
        assertEquals("a", ExprRenderer.render(COL_A));
    }

    @Test
    void columnRef_alias() {
        assertEquals("name AS n", ExprRenderer.render(new ColumnRef.Alias("name", "n")));
    }

    @Test
    void columnRef_variableBase() {
        assertEquals("t.id", ExprRenderer.render(new ColumnRef.VariableBase("t", "id")));
    }

    @Test
    void columnRef_variableAlias() {
        assertEquals("t.name AS n", ExprRenderer.render(new ColumnRef.VariableAlias("t", "name", "n")));
    }

    // ── Literals ──────────────────────────────────────────────────────────────

    @Test
    void literal_string() {
        assertEquals("'hello'", ExprRenderer.render(new Literal.StringLiteral("hello")));
    }

    @Test
    void literal_string_escapesQuotes() {
        assertEquals("'it''s'", ExprRenderer.render(new Literal.StringLiteral("it's")));
    }

    @Test
    void literal_int() {
        assertEquals("42", ExprRenderer.render(new Literal.IntLiteral(42)));
    }

    @Test
    void literal_long() {
        assertEquals("100", ExprRenderer.render(new Literal.LongLiteral(100L)));
    }

    @Test
    void literal_boolean_true() {
        assertEquals("TRUE", ExprRenderer.render(new Literal.BooleanLiteral(true)));
    }

    @Test
    void literal_boolean_false() {
        assertEquals("FALSE", ExprRenderer.render(new Literal.BooleanLiteral(false)));
    }

    @Test
    void literal_null() {
        assertEquals("NULL", ExprRenderer.render(new Literal.NullLiteral()));
    }

    @Test
    void literal_float() {
        assertEquals("1.5", ExprRenderer.render(new Literal.FloatLiteral(1.5f)));
    }

    @Test
    void literal_double() {
        assertEquals("3.14", ExprRenderer.render(new Literal.DoubleLiteral(3.14)));
    }

    @Test
    void literal_bigDecimal() {
        assertEquals("123.456", ExprRenderer.render(new Literal.BigDecimalLiteral(new BigDecimal("123.456"))));
    }

    @Test
    void literal_generic() {
        assertEquals("foo", ExprRenderer.render(new Literal.GenericLiteral("foo")));
    }

    // ── BinaryOp ──────────────────────────────────────────────────────────────

    @Test
    void binaryOp_plus() {
        assertEquals("(a + 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.PLUS, COL_A, ONE)));
    }

    @Test
    void binaryOp_minus() {
        assertEquals("(a - 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.MINUS, COL_A, ONE)));
    }

    @Test
    void binaryOp_multiply() {
        assertEquals("(a * b)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.MULTIPLY, COL_A, COL_B)));
    }

    @Test
    void binaryOp_div() {
        assertEquals("(a / b)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.DIV, COL_A, COL_B)));
    }

    @Test
    void binaryOp_and() {
        assertEquals("(a AND b)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.AND, COL_A, COL_B)));
    }

    @Test
    void binaryOp_or() {
        assertEquals("(a OR b)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.OR, COL_A, COL_B)));
    }

    @Test
    void binaryOp_eq() {
        assertEquals("(a = 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ONE)));
    }

    @Test
    void binaryOp_neq() {
        assertEquals("(a <> 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.NEQ, COL_A, ONE)));
    }

    @Test
    void binaryOp_gt() {
        assertEquals("(a > 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.GT, COL_A, ONE)));
    }

    @Test
    void binaryOp_gte() {
        assertEquals("(a >= 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.GTE, COL_A, ONE)));
    }

    @Test
    void binaryOp_lt() {
        assertEquals("(a < 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.LT, COL_A, ONE)));
    }

    @Test
    void binaryOp_lte() {
        assertEquals("(a <= 1)", ExprRenderer.render(new BinaryOp(BinaryOp.BinaryOperator.LTE, COL_A, ONE)));
    }

    @Test
    void binaryOp_like() {
        assertEquals("(a LIKE 'foo%')", ExprRenderer.render(
            new BinaryOp(BinaryOp.BinaryOperator.LIKE, COL_A, new Literal.StringLiteral("foo%"))));
    }

    @Test
    void binaryOp_notLike() {
        assertEquals("(a NOT LIKE 'foo%')", ExprRenderer.render(
            new BinaryOp(BinaryOp.BinaryOperator.NOT_LIKE, COL_A, new Literal.StringLiteral("foo%"))));
    }

    @Test
    void binaryOp_nested() {
        var inner = new BinaryOp(BinaryOp.BinaryOperator.PLUS, COL_A, COL_B);
        var outer = new BinaryOp(BinaryOp.BinaryOperator.EQ, inner, ONE);
        assertEquals("((a + b) = 1)", ExprRenderer.render(outer));
    }

    // ── UnaryOp ───────────────────────────────────────────────────────────────

    @Test
    void unaryOp_plus() {
        assertEquals("+a", ExprRenderer.render(new UnaryOp(UnaryOp.UnaryOperator.PLUS, COL_A)));
    }

    @Test
    void unaryOp_minus() {
        assertEquals("-a", ExprRenderer.render(new UnaryOp(UnaryOp.UnaryOperator.MINUS, COL_A)));
    }

    @Test
    void unaryOp_not() {
        assertEquals("NOT a", ExprRenderer.render(new UnaryOp(UnaryOp.UnaryOperator.NOT, COL_A)));
    }

    // ── IsNullExpr ────────────────────────────────────────────────────────────

    @Test
    void isNull() {
        assertEquals("a IS NULL", ExprRenderer.render(new IsNullExpr.IsNull(COL_A)));
    }

    @Test
    void isNotNull() {
        assertEquals("a IS NOT NULL", ExprRenderer.render(new IsNullExpr.IsNotNull(COL_A)));
    }

    // ── BetweenExpr ───────────────────────────────────────────────────────────

    @Test
    void between() {
        assertEquals("a BETWEEN 1 AND b", ExprRenderer.render(new BetweenExpr.Between(COL_A, ONE, COL_B)));
    }

    @Test
    void notBetween() {
        assertEquals("a NOT BETWEEN 1 AND b", ExprRenderer.render(new BetweenExpr.NotBetween(COL_A, ONE, COL_B)));
    }

    // ── InExpr ────────────────────────────────────────────────────────────────

    @Test
    void in_valueList() {
        var source = new InExpr.ValueList(List.of(ONE, new Literal.IntLiteral(2)));
        assertEquals("a IN (1, 2)", ExprRenderer.render(new InExpr.In(COL_A, source)));
    }

    @Test
    void notIn_valueList() {
        var source = new InExpr.ValueList(List.of(ONE));
        assertEquals("a NOT IN (1)", ExprRenderer.render(new InExpr.NotIn(COL_A, source)));
    }

    @Test
    void in_subquery() {
        var source = new InExpr.SubquerySource(tableSubquery("SELECT id FROM t"));
        assertEquals("a IN (SELECT 1)", ExprRenderer.render(new InExpr.In(COL_A, source)));
    }

    @Test
    void notIn_subquery() {
        var source = new InExpr.SubquerySource(tableSubquery("SELECT id FROM t"));
        assertEquals("a NOT IN (SELECT 1)", ExprRenderer.render(new InExpr.NotIn(COL_A, source)));
    }

    // ── CaseExpr ──────────────────────────────────────────────────────────────

    @Test
    void caseExpr() {
        var branch = new WhenThen(new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ONE), COL_B);
        assertEquals("CASE WHEN (a = 1) THEN b END", ExprRenderer.render(new CaseExpr.Case(List.of(branch))));
    }

    @Test
    void caseExpr_withElse() {
        var branch = new WhenThen(new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ONE), COL_B);
        assertEquals("CASE WHEN (a = 1) THEN b ELSE c END",
            ExprRenderer.render(new CaseExpr.CaseElse(List.of(branch), COL_C)));
    }

    @Test
    void simpleCase() {
        var branch = new WhenThen(ONE, COL_B);
        assertEquals("CASE a WHEN 1 THEN b END",
            ExprRenderer.render(new CaseExpr.SimpleCase(COL_A, List.of(branch))));
    }

    @Test
    void simpleCaseElse() {
        var branch = new WhenThen(ONE, COL_B);
        assertEquals("CASE a WHEN 1 THEN b ELSE c END",
            ExprRenderer.render(new CaseExpr.SimpleCaseElse(COL_A, List.of(branch), COL_C)));
    }

    @Test
    void caseExpr_multipleBranches() {
        var b1 = new WhenThen(new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ONE), COL_B);
        var b2 = new WhenThen(new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, new Literal.IntLiteral(2)), COL_C);
        assertEquals("CASE WHEN (a = 1) THEN b WHEN (a = 2) THEN c END",
            ExprRenderer.render(new CaseExpr.Case(List.of(b1, b2))));
    }

    // ── QuantifiedExpr ────────────────────────────────────────────────────────

    @Test
    void quantifiedAll_eq() {
        var expr = new QuantifiedExpr.All(QuantifiedExpr.ComparisonOperator.EQ, COL_A, tableSubquery("SELECT price FROM t"));
        assertEquals("a = ALL (SELECT 1)", ExprRenderer.render(expr));
    }

    @Test
    void quantifiedAll_gt() {
        var expr = new QuantifiedExpr.All(QuantifiedExpr.ComparisonOperator.GT, COL_A, tableSubquery("SELECT price FROM t"));
        assertEquals("a > ALL (SELECT 1)", ExprRenderer.render(expr));
    }

    @Test
    void quantifiedAny_eq() {
        var expr = new QuantifiedExpr.Any(QuantifiedExpr.ComparisonOperator.EQ, COL_A, tableSubquery("SELECT price FROM t"));
        assertEquals("a = ANY (SELECT 1)", ExprRenderer.render(expr));
    }

    @Test
    void quantifiedAny_lt() {
        var expr = new QuantifiedExpr.Any(QuantifiedExpr.ComparisonOperator.LT, COL_A, tableSubquery("SELECT price FROM t"));
        assertEquals("a < ANY (SELECT 1)", ExprRenderer.render(expr));
    }

    // ── Exists & ScalarSubquery ───────────────────────────────────────────────

    @Test
    void exists() {
        assertEquals("EXISTS (SELECT 1)", ExprRenderer.render(new Exists(tableSubquery("SELECT 1 FROM t"))));
    }

    @Test
    void scalarSubquery() {
        var sub = new Subquery.Scalar(TestFixtures.stubJqRead());
        assertEquals("(SELECT 1)", ExprRenderer.render(sub));
    }

    // ── Func.Aggregate ────────────────────────────────────────────────────────

    @Test
    void func_countAll() {
        assertEquals("COUNT(*)", ExprRenderer.render(new Func.CountAll()));
    }

    @Test
    void func_count() {
        assertEquals("COUNT(a)", ExprRenderer.render(new Func.Count(COL_A, false)));
    }

    @Test
    void func_countDistinct() {
        assertEquals("COUNT(DISTINCT a)", ExprRenderer.render(new Func.Count(COL_A, true)));
    }

    @Test
    void func_sum() {
        assertEquals("SUM(a)", ExprRenderer.render(new Func.Sum(COL_A, false)));
    }

    @Test
    void func_sumDistinct() {
        assertEquals("SUM(DISTINCT a)", ExprRenderer.render(new Func.Sum(COL_A, true)));
    }

    @Test
    void func_avg() {
        assertEquals("AVG(a)", ExprRenderer.render(new Func.Avg(COL_A, false)));
    }

    @Test
    void func_avgDistinct() {
        assertEquals("AVG(DISTINCT a)", ExprRenderer.render(new Func.Avg(COL_A, true)));
    }

    @Test
    void func_min() {
        assertEquals("MIN(a)", ExprRenderer.render(new Func.Min(COL_A)));
    }

    @Test
    void func_max() {
        assertEquals("MAX(a)", ExprRenderer.render(new Func.Max(COL_A)));
    }

    // ── Func.Text ─────────────────────────────────────────────────────────────

    @Test
    void func_upper() {
        assertEquals("UPPER(a)", ExprRenderer.render(new Func.Upper(COL_A)));
    }

    @Test
    void func_lower() {
        assertEquals("LOWER(a)", ExprRenderer.render(new Func.Lower(COL_A)));
    }

    @Test
    void func_trim() {
        assertEquals("TRIM(a)", ExprRenderer.render(new Func.Trim(COL_A)));
    }

    @Test
    void func_length() {
        assertEquals("LENGTH(a)", ExprRenderer.render(new Func.Length(COL_A)));
    }

    @Test
    void func_substring() {
        assertEquals("SUBSTRING(a FROM b FOR c)", ExprRenderer.render(new Func.Substring(COL_A, COL_B, COL_C)));
    }

    // ── Func.Temporal ─────────────────────────────────────────────────────────

    @Test
    void func_currentDate() {
        assertEquals("CURRENT_DATE", ExprRenderer.render(new Func.CurrentDate()));
    }

    @Test
    void func_currentTimestamp() {
        assertEquals("CURRENT_TIMESTAMP", ExprRenderer.render(new Func.CurrentTimestamp()));
    }

    @Test
    void func_extract_year() {
        assertEquals("EXTRACT(YEAR FROM a)", ExprRenderer.render(new Func.Extract(Func.TemporalField.YEAR, COL_A)));
    }

    @Test
    void func_extract_month() {
        assertEquals("EXTRACT(MONTH FROM a)", ExprRenderer.render(new Func.Extract(Func.TemporalField.MONTH, COL_A)));
    }

    @Test
    void func_extract_day() {
        assertEquals("EXTRACT(DAY FROM a)", ExprRenderer.render(new Func.Extract(Func.TemporalField.DAY, COL_A)));
    }

    @Test
    void func_extract_hour() {
        assertEquals("EXTRACT(HOUR FROM a)", ExprRenderer.render(new Func.Extract(Func.TemporalField.HOUR, COL_A)));
    }

    @Test
    void func_extract_minute() {
        assertEquals("EXTRACT(MINUTE FROM a)", ExprRenderer.render(new Func.Extract(Func.TemporalField.MINUTE, COL_A)));
    }

    @Test
    void func_extract_second() {
        assertEquals("EXTRACT(SECOND FROM a)", ExprRenderer.render(new Func.Extract(Func.TemporalField.SECOND, COL_A)));
    }

    // ── Func.Conditional ──────────────────────────────────────────────────────

    @Test
    void func_coalesce() {
        assertEquals("COALESCE(a, b)", ExprRenderer.render(new Func.Coalesce(List.of(COL_A, COL_B))));
    }

    @Test
    void func_coalesce_three() {
        assertEquals("COALESCE(a, b, c)", ExprRenderer.render(new Func.Coalesce(List.of(COL_A, COL_B, COL_C))));
    }

    @Test
    void func_nullif() {
        assertEquals("NULLIF(a, b)", ExprRenderer.render(new Func.NullIf(COL_A, COL_B)));
    }

    // ── Func.Numeric ──────────────────────────────────────────────────────────

    @Test
    void func_abs() {
        assertEquals("ABS(a)", ExprRenderer.render(new Func.Abs(COL_A)));
    }

    @Test
    void func_round() {
        assertEquals("ROUND(a, b)", ExprRenderer.render(new Func.Round(COL_A, COL_B)));
    }

    @Test
    void func_floor() {
        assertEquals("FLOOR(a)", ExprRenderer.render(new Func.Floor(COL_A)));
    }

    @Test
    void func_ceil() {
        assertEquals("CEIL(a)", ExprRenderer.render(new Func.Ceil(COL_A)));
    }

    @Test
    void func_power() {
        assertEquals("POWER(a, b)", ExprRenderer.render(new Func.Power(COL_A, COL_B)));
    }

    @Test
    void func_sqrt() {
        assertEquals("SQRT(a)", ExprRenderer.render(new Func.Sqrt(COL_A)));
    }

    @Test
    void func_mod() {
        assertEquals("MOD(a, b)", ExprRenderer.render(new Func.Mod(COL_A, COL_B)));
    }
}