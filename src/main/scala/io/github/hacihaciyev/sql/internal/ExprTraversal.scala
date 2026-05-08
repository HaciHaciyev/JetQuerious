package io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.sql.expressions.*

import scala.jdk.CollectionConverters.*

object ExprTraversal {

    def collectCrefs(expr: Expr): List[ColumnRef] = expr match {
        case col: ColumnRef              => List(col)
        case op: BinaryOp                => collectCrefs(op.left()) ++ collectCrefs(op.right())
        case op: UnaryOp                 => collectCrefs(op.expr())
        case e: IsNullExpr.IsNull        => collectCrefs(e.operand())
        case e: IsNullExpr.IsNotNull     => collectCrefs(e.operand())
        case e: BetweenExpr.Between      => collectCrefs(e.operand()) ++ collectCrefs(e.low()) ++ collectCrefs(e.high())
        case e: BetweenExpr.NotBetween   => collectCrefs(e.operand()) ++ collectCrefs(e.low()) ++ collectCrefs(e.high())
        case e: InExpr.In                => collectCrefs(e.operand()) ++ collectInSource(e.source())
        case e: InExpr.NotIn             => collectCrefs(e.operand()) ++ collectInSource(e.source())
        case e: CaseExpr.Case            => collectBranches(e.branches().asScala.toList)
        case e: CaseExpr.CaseElse        => collectBranches(e.branches().asScala.toList) ++ collectCrefs(e.elseBranch())
        case e: CaseExpr.SimpleCase      => collectCrefs(e.operand()) ++ collectBranches(e.branches().asScala.toList)
        case e: CaseExpr.SimpleCaseElse  => collectCrefs(e.operand()) ++ collectBranches(e.branches().asScala.toList) ++ collectCrefs(e.elseBranch())
        case e: QuantifiedExpr.All       => collectCrefs(e.operand())
        case e: QuantifiedExpr.Any       => collectCrefs(e.operand())
        case f: Func                     => collectFunc(f)
        case _: Literal                  => List()
        case _: Exists                   => List()
        case _: Subquery.ScalarSubquery  => List()
    }

    def collectAllCrefs(exprs: List[Expr]): List[ColumnRef] =
        exprs.flatMap(collectCrefs)

    private def collectInSource(src: InExpr.InSource): List[ColumnRef] = src match {
        case vl: InExpr.ValueList     => vl.values().asScala.toList.flatMap(collectCrefs)
        case _: InExpr.SubquerySource => List()
    }

    private def collectBranches(branches: List[CaseExpr.WhenThen]): List[ColumnRef] =
        branches.flatMap(b => collectCrefs(b.condition()) ++ collectCrefs(b.result()))

    private def collectFunc(f: Func): List[ColumnRef] = f match {
        case e: Func.Count            => collectCrefs(e.expr())
        case e: Func.Sum              => collectCrefs(e.expr())
        case e: Func.Avg              => collectCrefs(e.expr())
        case e: Func.Min              => collectCrefs(e.expr())
        case e: Func.Max              => collectCrefs(e.expr())
        case e: Func.Upper            => collectCrefs(e.value())
        case e: Func.Lower            => collectCrefs(e.value())
        case e: Func.Trim             => collectCrefs(e.value())
        case e: Func.Length           => collectCrefs(e.value())
        case e: Func.Substring        => collectCrefs(e.value()) ++ collectCrefs(e.start()) ++ collectCrefs(e.length())
        case e: Func.Extract          => collectCrefs(e.source())
        case e: Func.Coalesce         => e.values().asScala.toList.flatMap(collectCrefs)
        case e: Func.NullIf           => collectCrefs(e.first()) ++ collectCrefs(e.second())
        case e: Func.Abs              => collectCrefs(e.value())
        case e: Func.Round            => collectCrefs(e.value()) ++ collectCrefs(e.precision())
        case e: Func.Floor            => collectCrefs(e.value())
        case e: Func.Ceil             => collectCrefs(e.value())
        case e: Func.Power            => collectCrefs(e.base()) ++ collectCrefs(e.exponent())
        case e: Func.Sqrt             => collectCrefs(e.value())
        case e: Func.Mod              => collectCrefs(e.left()) ++ collectCrefs(e.right())
        case e: Func.Cast             => collectCrefs(e.value())
        case _: Func.CountAll         => List()
        case _: Func.CurrentDate      => List()
        case _: Func.CurrentTimestamp => List()
    }
}