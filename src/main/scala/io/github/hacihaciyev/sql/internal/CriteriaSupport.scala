package io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.sql.expressions.*

import scala.jdk.CollectionConverters.*

object CriteriaSupport {

    def flattenAnd(expr: Expr): List[Expr] = expr match {
        case op: BinaryOp if op.operator() == BinaryOp.BinaryOperator.AND =>
            flattenAnd(op.left()) ++ flattenAnd(op.right())
        case other => List(other)
    }

    def containsOptional(expr: Expr): Boolean = expr match {
        case _: Literal.OptionalPlaceholderLiteral => true
        case _: Literal                            => false
        case _: ColumnRef                          => false
        case op: BinaryOp                          => containsOptional(op.left()) || containsOptional(op.right())
        case op: UnaryOp                           => containsOptional(op.expr())
        case e: IsNullExpr.IsNull                  => containsOptional(e.operand())
        case e: IsNullExpr.IsNotNull               => containsOptional(e.operand())
        case e: BetweenExpr.Between                => containsOptional(e.operand()) || containsOptional(e.low()) || containsOptional(e.high())
        case e: BetweenExpr.NotBetween             => containsOptional(e.operand()) || containsOptional(e.low()) || containsOptional(e.high())
        case e: InExpr.In                          => containsOptional(e.operand()) || containsOptionalInSource(e.source())
        case e: InExpr.NotIn                       => containsOptional(e.operand()) || containsOptionalInSource(e.source())
        case e: CaseExpr.Case                      => containsOptionalBranches(e.branches())
        case e: CaseExpr.CaseElse                  => containsOptionalBranches(e.branches()) || containsOptional(e.elseBranch())
        case e: CaseExpr.SimpleCase                => containsOptional(e.operand()) || containsOptionalBranches(e.branches())
        case e: CaseExpr.SimpleCaseElse            => containsOptional(e.operand()) || containsOptionalBranches(e.branches()) || containsOptional(e.elseBranch())
        case e: QuantifiedExpr.All                 => containsOptional(e.operand())
        case e: QuantifiedExpr.Any                 => containsOptional(e.operand())
        case _: Exists                             => false
        case _: Subquery.Scalar                    => false
        case f: Func                               => containsOptionalFunc(f)
    }

    def containsOptionalAny(exprs: List[Expr]): Boolean = exprs.exists(containsOptional)

    private def containsOptionalInSource(src: InExpr.InSource): Boolean = src match {
        case vl: InExpr.ValueList     => vl.values().asScala.exists(containsOptional)
        case _: InExpr.SubquerySource => false
    }

    private def containsOptionalBranches(branches: java.util.List[CaseExpr.WhenThen]): Boolean =
        branches.asScala.exists(b => containsOptional(b.condition()) || containsOptional(b.result()))

    private def containsOptionalFunc(f: Func): Boolean = f match {
        case e: Func.Count            => containsOptional(e.expr())
        case e: Func.Sum              => containsOptional(e.expr())
        case e: Func.Avg              => containsOptional(e.expr())
        case e: Func.Min              => containsOptional(e.expr())
        case e: Func.Max              => containsOptional(e.expr())
        case e: Func.Upper            => containsOptional(e.value())
        case e: Func.Lower            => containsOptional(e.value())
        case e: Func.Trim             => containsOptional(e.value())
        case e: Func.Length           => containsOptional(e.value())
        case e: Func.Substring        => containsOptional(e.value()) || containsOptional(e.start()) || containsOptional(e.length())
        case e: Func.Extract          => containsOptional(e.source())
        case e: Func.Coalesce         => e.values().asScala.exists(containsOptional)
        case e: Func.NullIf           => containsOptional(e.first()) || containsOptional(e.second())
        case e: Func.Abs              => containsOptional(e.value())
        case e: Func.Round            => containsOptional(e.value()) || containsOptional(e.precision())
        case e: Func.Floor            => containsOptional(e.value())
        case e: Func.Ceil             => containsOptional(e.value())
        case e: Func.Power            => containsOptional(e.base()) || containsOptional(e.exponent())
        case e: Func.Sqrt             => containsOptional(e.value())
        case e: Func.Mod              => containsOptional(e.left()) || containsOptional(e.right())
        case e: Func.Cast             => containsOptional(e.value())
        case _: Func.CountAll         => false
        case _: Func.CurrentDate      => false
        case _: Func.CurrentTimestamp => false
    }
}