package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.sql.expressions.*;
import io.github.hacihaciyev.sql.expressions.CaseExpr.WhenThen;
import io.github.hacihaciyev.types.SQLType;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExprTraversalTest {

    private static final ColumnRef.Base COL_A = new ColumnRef.Base("a");
    private static final ColumnRef.Base COL_B = new ColumnRef.Base("b");
    private static final ColumnRef.Base COL_C = new ColumnRef.Base("c");
    private static final Literal.IntLiteral ONE = new Literal.IntLiteral(1);

    private static Subquery.Table tableSubquery() {
        return new Subquery.Table(TestFixtures.stubJqRead());
    }

    @Test
    void literal_returnsEmpty() {
        assertTrue(ExprTraversal.collectCrefs(ONE).isEmpty());
    }

    @Test
    void exists_returnsEmpty() {
        assertTrue(ExprTraversal.collectCrefs(new Exists(tableSubquery())).isEmpty());
    }

    @Test
    void scalarSubquery_returnsEmpty() {
        var sub = new Subquery.Scalar(TestFixtures.stubJqRead());
        assertTrue(ExprTraversal.collectCrefs(sub).isEmpty());
    }

    @Test
    void columnRef_base_returnsItself() {
        var result = ExprTraversal.collectCrefs(COL_A);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void columnRef_variableBase_returnsItself() {
        var col    = new ColumnRef.VariableBase("t", "id");
        var result = ExprTraversal.collectCrefs(col);
        assertEquals(1, result.size());
        assertEquals(col, result.apply(0));
    }

    @Test
    void columnRef_alias_returnsItself() {
        var col    = new ColumnRef.Alias("name", "n");
        var result = ExprTraversal.collectCrefs(col);
        assertEquals(1, result.size());
        assertEquals(col, result.apply(0));
    }

    @Test
    void columnRef_variableAlias_returnsItself() {
        var col    = new ColumnRef.VariableAlias("t", "name", "n");
        var result = ExprTraversal.collectCrefs(col);
        assertEquals(1, result.size());
        assertEquals(col, result.apply(0));
    }

    @Test
    void binaryOp_collectsBothSides() {
        var expr   = new BinaryOp(BinaryOp.BinaryOperator.PLUS, COL_A, COL_B);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(2, result.size());
        assertTrue(result.contains(COL_A));
        assertTrue(result.contains(COL_B));
    }

    @Test
    void binaryOp_nested_collectsAll() {
        var inner  = new BinaryOp(BinaryOp.BinaryOperator.PLUS, COL_A, COL_B);
        var outer  = new BinaryOp(BinaryOp.BinaryOperator.PLUS, inner, COL_C);
        var result = ExprTraversal.collectCrefs(outer);
        assertEquals(3, result.size());
        assertTrue(result.contains(COL_A));
        assertTrue(result.contains(COL_B));
        assertTrue(result.contains(COL_C));
    }

    @Test
    void binaryOp_withLiteral_collectsOnlyRef() {
        var expr   = new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ONE);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void unaryOp_collectsInner() {
        var expr   = new UnaryOp(UnaryOp.UnaryOperator.NOT, COL_A);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void unaryOp_withLiteral_returnsEmpty() {
        var expr   = new UnaryOp(UnaryOp.UnaryOperator.MINUS, ONE);
        var result = ExprTraversal.collectCrefs(expr);
        assertTrue(result.isEmpty());
    }

    @Test
    void isNull_collectsOperand() {
        var result = ExprTraversal.collectCrefs(new IsNullExpr.IsNull(COL_A));
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void isNotNull_collectsOperand() {
        var result = ExprTraversal.collectCrefs(new IsNullExpr.IsNotNull(COL_A));
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void between_collectsAll() {
        var expr   = new BetweenExpr.Between(COL_A, COL_B, COL_C);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(3, result.size());
    }

    @Test
    void notBetween_collectsAll() {
        var expr   = new BetweenExpr.NotBetween(COL_A, ONE, ONE);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void in_valueList_collectsAll() {
        var source = new InExpr.ValueList(List.of(COL_B, COL_C));
        var expr   = new InExpr.In(COL_A, source);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(3, result.size());
    }

    @Test
    void in_subquerySource_collectsOnlyOperand() {
        var source = new InExpr.SubquerySource(tableSubquery());
        var expr   = new InExpr.In(COL_A, source);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void notIn_valueList_collectsAll() {
        var source = new InExpr.ValueList(List.of(COL_B));
        var expr   = new InExpr.NotIn(COL_A, source);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(2, result.size());
    }

    @Test
    void case_collectsBranches() {
        var branch = new WhenThen(COL_A, COL_B);
        var expr   = new CaseExpr.Case(List.of(branch));
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(2, result.size());
    }

    @Test
    void caseElse_collectsBranchesAndElse() {
        var branch = new WhenThen(COL_A, COL_B);
        var expr   = new CaseExpr.CaseElse(List.of(branch), COL_C);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(3, result.size());
    }

    @Test
    void simpleCase_collectsOperandAndBranches() {
        var branch = new WhenThen(ONE, COL_B);
        var expr   = new CaseExpr.SimpleCase(COL_A, List.of(branch));
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(2, result.size());
        assertTrue(result.contains(COL_A));
        assertTrue(result.contains(COL_B));
    }

    @Test
    void simpleCaseElse_collectsAll() {
        var branch = new WhenThen(ONE, COL_B);
        var expr   = new CaseExpr.SimpleCaseElse(COL_A, List.of(branch), COL_C);
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(3, result.size());
    }

    @Test
    void quantifiedAll_collectsOnlyOperand() {
        var expr   = new QuantifiedExpr.All(QuantifiedExpr.ComparisonOperator.EQ, COL_A, tableSubquery());
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void quantifiedAny_collectsOnlyOperand() {
        var expr   = new QuantifiedExpr.Any(QuantifiedExpr.ComparisonOperator.GT, COL_A, tableSubquery());
        var result = ExprTraversal.collectCrefs(expr);
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void func_countAll_returnsEmpty() {
        assertTrue(ExprTraversal.collectCrefs(new Func.CountAll()).isEmpty());
    }

    @Test
    void func_currentDate_returnsEmpty() {
        assertTrue(ExprTraversal.collectCrefs(new Func.CurrentDate()).isEmpty());
    }

    @Test
    void func_currentTimestamp_returnsEmpty() {
        assertTrue(ExprTraversal.collectCrefs(new Func.CurrentTimestamp()).isEmpty());
    }

    @Test
    void func_count_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Count(COL_A, false));
        assertEquals(1, result.size());
        assertEquals(COL_A, result.apply(0));
    }

    @Test
    void func_countDistinct_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Count(COL_A, true));
        assertEquals(1, result.size());
    }

    @Test
    void func_sum_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Sum(COL_A, false));
        assertEquals(1, result.size());
    }

    @Test
    void func_avg_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Avg(COL_A, false));
        assertEquals(1, result.size());
    }

    @Test
    void func_min_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Min(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_max_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Max(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_upper_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Upper(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_lower_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Lower(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_trim_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Trim(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_length_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Length(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_substring_collectsAll() {
        var result = ExprTraversal.collectCrefs(new Func.Substring(COL_A, COL_B, COL_C));
        assertEquals(3, result.size());
    }

    @Test
    void func_extract_collectsSource() {
        var result = ExprTraversal.collectCrefs(new Func.Extract(Func.TemporalField.YEAR, COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_coalesce_collectsAll() {
        var result = ExprTraversal.collectCrefs(new Func.Coalesce(List.of(COL_A, COL_B)));
        assertEquals(2, result.size());
    }

    @Test
    void func_nullif_collectsBoth() {
        var result = ExprTraversal.collectCrefs(new Func.NullIf(COL_A, COL_B));
        assertEquals(2, result.size());
    }

    @Test
    void func_abs_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Abs(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_round_collectsBoth() {
        var result = ExprTraversal.collectCrefs(new Func.Round(COL_A, COL_B));
        assertEquals(2, result.size());
    }

    @Test
    void func_floor_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Floor(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_ceil_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Ceil(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_power_collectsBoth() {
        var result = ExprTraversal.collectCrefs(new Func.Power(COL_A, COL_B));
        assertEquals(2, result.size());
    }

    @Test
    void func_sqrt_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Sqrt(COL_A));
        assertEquals(1, result.size());
    }

    @Test
    void func_mod_collectsBoth() {
        var result = ExprTraversal.collectCrefs(new Func.Mod(COL_A, COL_B));
        assertEquals(2, result.size());
    }

    @Test
    void func_cast_collectsExpr() {
        var result = ExprTraversal.collectCrefs(new Func.Cast(COL_A, SQLType.ANY));
        assertEquals(1, result.size());
    }

    @Test
    void collectAllCrefs_flattensMultipleExprs() {
        var exprs  = scala.jdk.javaapi.CollectionConverters.asScala(List.<Expr>of(COL_A, COL_B, COL_C)).toList();
        var result = ExprTraversal.collectAllCrefs(exprs);
        assertEquals(3, result.size());
    }

    @Test
    void collectAllCrefs_emptyList_returnsEmpty() {
        var exprs  = scala.jdk.javaapi.CollectionConverters.asScala(List.<Expr>of()).toList();
        var result = ExprTraversal.collectAllCrefs(exprs);
        assertTrue(result.isEmpty());
    }

    @Nested
    class CollectPlaceholders {

        private static Literal.PlaceholderLiteral ph(Class<?> type) {
            return new Literal.PlaceholderLiteral(type);
        }

        private static List<Class<?>> collect(Expr expr) {
            return scala.jdk.javaapi.CollectionConverters.asJava(
                ExprTraversal.collectPlaceholders(expr)
            );
        }

        @Test
        void placeholder_returnsItsType() {
            var result = collect(ph(Integer.class));
            assertEquals(1, result.size());
            assertEquals(Integer.class, result.get(0));
        }

        @Test
        void otherLiteral_returnsEmpty() {
            assertTrue(collect(ONE).isEmpty());
            assertTrue(collect(new Literal.StringLiteral("x")).isEmpty());
            assertTrue(collect(new Literal.NullLiteral()).isEmpty());
            assertTrue(collect(new Literal.BooleanLiteral(true)).isEmpty());
        }

        @Test
        void columnRef_returnsEmpty() {
            assertTrue(collect(COL_A).isEmpty());
        }

        @Test
        void exists_returnsEmpty() {
            assertTrue(collect(new Exists(tableSubquery())).isEmpty());
        }

        @Test
        void scalarSubquery_returnsEmpty() {
            assertTrue(collect(new Subquery.Scalar(TestFixtures.stubJqRead())).isEmpty());
        }

        @Test
        void binaryOp_bothPlaceholders_returnsBoth() {
            var expr   = new BinaryOp(BinaryOp.BinaryOperator.EQ, ph(Integer.class), ph(String.class));
            var result = collect(expr);
            assertEquals(2, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(String.class, result.get(1));
        }

        @Test
        void binaryOp_leftPlaceholder_rightCol_returnsOne() {
            var expr   = new BinaryOp(BinaryOp.BinaryOperator.EQ, ph(Long.class), COL_A);
            var result = collect(expr);
            assertEquals(1, result.size());
            assertEquals(Long.class, result.get(0));
        }

        @Test
        void binaryOp_leftCol_rightPlaceholder_returnsOne() {
            var expr   = new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ph(String.class));
            var result = collect(expr);
            assertEquals(1, result.size());
            assertEquals(String.class, result.get(0));
        }

        @Test
        void binaryOp_noPlaceholders_returnsEmpty() {
            var expr = new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ONE);
            assertTrue(collect(expr).isEmpty());
        }

        @Test
        void binaryOp_nested_preservesOrder() {
            var left  = new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ph(Integer.class));
            var right = new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_B, ph(String.class));
            var expr  = new BinaryOp(BinaryOp.BinaryOperator.AND, left, right);
            var result = collect(expr);
            assertEquals(2, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(String.class, result.get(1));
        }

        @Test
        void unaryOp_withPlaceholder_returnsIt() {
            var result = collect(new UnaryOp(UnaryOp.UnaryOperator.NOT, ph(Boolean.class)));
            assertEquals(1, result.size());
            assertEquals(Boolean.class, result.get(0));
        }

        @Test
        void isNull_withPlaceholder_returnsIt() {
            var result = collect(new IsNullExpr.IsNull(ph(String.class)));
            assertEquals(1, result.size());
        }

        @Test
        void isNotNull_withPlaceholder_returnsIt() {
            var result = collect(new IsNullExpr.IsNotNull(ph(String.class)));
            assertEquals(1, result.size());
        }

        @Test
        void between_allPlaceholders_returnsThree() {
            var result = collect(new BetweenExpr.Between(ph(Integer.class), ph(Integer.class), ph(Integer.class)));
            assertEquals(3, result.size());
        }

        @Test
        void between_preservesOrder_operandLowHigh() {
            var result = collect(new BetweenExpr.Between(ph(Integer.class), ph(Long.class), ph(Short.class)));
            assertEquals(3, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(Long.class,    result.get(1));
            assertEquals(Short.class,   result.get(2));
        }

        @Test
        void notBetween_preservesOrder() {
            var result = collect(new BetweenExpr.NotBetween(ph(Integer.class), ph(Long.class), ph(Short.class)));
            assertEquals(3, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(Long.class,    result.get(1));
            assertEquals(Short.class,   result.get(2));
        }

        @Test
        void in_valueList_withPlaceholders_collectsAll() {
            var source = new InExpr.ValueList(List.of(ph(Integer.class), ph(Long.class)));
            var expr   = new InExpr.In(ph(String.class), source);
            var result = collect(expr);
            assertEquals(3, result.size());
            assertEquals(String.class,  result.get(0));
            assertEquals(Integer.class, result.get(1));
            assertEquals(Long.class,    result.get(2));
        }

        @Test
        void in_subquerySource_collectsOnlyOperand() {
            var source = new InExpr.SubquerySource(tableSubquery());
            var result = collect(new InExpr.In(ph(Integer.class), source));
            assertEquals(1, result.size());
            assertEquals(Integer.class, result.get(0));
        }

        @Test
        void notIn_valueList_withPlaceholders() {
            var source = new InExpr.ValueList(List.of(ph(Integer.class)));
            var result = collect(new InExpr.NotIn(ph(String.class), source));
            assertEquals(2, result.size());
            assertEquals(String.class,  result.get(0));
            assertEquals(Integer.class, result.get(1));
        }

        @Test
        void case_branchesWithPlaceholders() {
            var branch = new WhenThen(ph(Boolean.class), ph(String.class));
            var result = collect(new CaseExpr.Case(List.of(branch)));
            assertEquals(2, result.size());
            assertEquals(Boolean.class, result.get(0));
            assertEquals(String.class,  result.get(1));
        }

        @Test
        void caseElse_collectsAll() {
            var branch = new WhenThen(ph(Boolean.class), ph(String.class));
            var result = collect(new CaseExpr.CaseElse(List.of(branch), ph(Integer.class)));
            assertEquals(3, result.size());
            assertEquals(Boolean.class,  result.get(0));
            assertEquals(String.class,   result.get(1));
            assertEquals(Integer.class,  result.get(2));
        }

        @Test
        void simpleCase_operandAndBranches() {
            var branch = new WhenThen(ph(Boolean.class), ph(String.class));
            var result = collect(new CaseExpr.SimpleCase(ph(Integer.class), List.of(branch)));
            assertEquals(3, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(Boolean.class, result.get(1));
            assertEquals(String.class,  result.get(2));
        }

        @Test
        void simpleCaseElse_collectsAll() {
            var branch = new WhenThen(ph(Boolean.class), ph(String.class));
            var result = collect(new CaseExpr.SimpleCaseElse(ph(Integer.class), List.of(branch), ph(Long.class)));
            assertEquals(4, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(Boolean.class, result.get(1));
            assertEquals(String.class,  result.get(2));
            assertEquals(Long.class,    result.get(3));
        }

        @Test
        void quantifiedAll_collectsOnlyOperand() {
            var result = collect(new QuantifiedExpr.All(
                QuantifiedExpr.ComparisonOperator.EQ, ph(Integer.class), tableSubquery()));
            assertEquals(1, result.size());
            assertEquals(Integer.class, result.get(0));
        }

        @Test
        void quantifiedAny_collectsOnlyOperand() {
            var result = collect(new QuantifiedExpr.Any(
                QuantifiedExpr.ComparisonOperator.GT, ph(Integer.class), tableSubquery()));
            assertEquals(1, result.size());
            assertEquals(Integer.class, result.get(0));
        }

        @Test
        void func_countAll_returnsEmpty() {
            assertTrue(collect(new Func.CountAll()).isEmpty());
        }

        @Test
        void func_count_withPlaceholder() {
            var result = collect(new Func.Count(ph(Integer.class), false));
            assertEquals(1, result.size());
            assertEquals(Integer.class, result.get(0));
        }

        @Test
        void func_sum_withPlaceholder() {
            var result = collect(new Func.Sum(ph(Long.class), false));
            assertEquals(1, result.size());
        }

        @Test
        void func_coalesce_withPlaceholders_preservesOrder() {
            var result = collect(new Func.Coalesce(List.of(ph(Integer.class), ph(String.class))));
            assertEquals(2, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(String.class,  result.get(1));
        }

        @Test
        void func_nullif_withPlaceholders() {
            var result = collect(new Func.NullIf(ph(Integer.class), ph(Long.class)));
            assertEquals(2, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(Long.class,    result.get(1));
        }

        @Test
        void func_round_withPlaceholders() {
            var result = collect(new Func.Round(ph(Double.class), ph(Integer.class)));
            assertEquals(2, result.size());
            assertEquals(Double.class,  result.get(0));
            assertEquals(Integer.class, result.get(1));
        }

        @Test
        void func_substring_withPlaceholders_preservesOrder() {
            var result = collect(new Func.Substring(ph(String.class), ph(Integer.class), ph(Integer.class)));
            assertEquals(3, result.size());
            assertEquals(String.class,  result.get(0));
            assertEquals(Integer.class, result.get(1));
            assertEquals(Integer.class, result.get(2));
        }

        @Test
        void func_cast_withPlaceholder() {
            var result = collect(new Func.Cast(ph(Integer.class), SQLType.ANY));
            assertEquals(1, result.size());
            assertEquals(Integer.class, result.get(0));
        }

        @Test
        void collectAllPlaceholders_flattensAndPreservesOrder() {
            var exprs = scala.jdk.javaapi.CollectionConverters.asScala(List.<Expr>of(
                new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_A, ph(Integer.class)),
                new BinaryOp(BinaryOp.BinaryOperator.EQ, COL_B, ph(String.class))
            )).toList();
            var result = scala.jdk.javaapi.CollectionConverters.asJava(
                ExprTraversal.collectAllPlaceholders(exprs)
            );
            assertEquals(2, result.size());
            assertEquals(Integer.class, result.get(0));
            assertEquals(String.class,  result.get(1));
        }

        @Test
        void collectAllPlaceholders_emptyList_returnsEmpty() {
            var exprs  = scala.jdk.javaapi.CollectionConverters.asScala(List.<Expr>of()).toList();
            var result = scala.jdk.javaapi.CollectionConverters.asJava(
                ExprTraversal.collectAllPlaceholders(exprs)
            );
            assertTrue(result.isEmpty());
        }
    }
}