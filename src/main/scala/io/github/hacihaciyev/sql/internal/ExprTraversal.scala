package io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.sql.expressions.*
import io.github.hacihaciyev.sql.value_objects.Projection
import io.github.hacihaciyev.sql.internal.value_objects.Ref

import scala.jdk.CollectionConverters.*

object ExprTraversal {

    def refsToExprsExcludingWildcards(refs: List[Ref]): List[Expr] =
        refs.flatMap {
            case rn: Ref.Named      => resolveProjection(rn.value)
            case ri: Ref.Indexed    => resolveProjection(ri.value)
        }
    
    private def resolveProjection(proj: Projection): List[Expr] = proj match {
        case pb: Projection.Base              => List(pb.expr)
        case pa: Projection.Aliased           => List(pa.expr)
        case _ : Projection.Wildcard          => List()
        case _ : Projection.QualifiedWildcard => List()
    }

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
        case _: Subquery.Scalar          => List()
    }

    def collectAllCrefs(exprs: List[Expr]): List[ColumnRef] =
        exprs.flatMap(collectCrefs)

    def collectPlaceholders(expr: Expr): List[Class[?]] = expr match {
        case p: Literal.PlaceholderLiteral => List(p.`type`())
        case _: Literal                    => List()
        case _: ColumnRef                  => List()
        case op: BinaryOp                  => collectPlaceholders(op.left()) ++ collectPlaceholders(op.right())
        case op: UnaryOp                   => collectPlaceholders(op.expr())
        case e: IsNullExpr.IsNull          => collectPlaceholders(e.operand())
        case e: IsNullExpr.IsNotNull       => collectPlaceholders(e.operand())
        case e: BetweenExpr.Between        => collectPlaceholders(e.operand()) ++ collectPlaceholders(e.low()) ++ collectPlaceholders(e.high())
        case e: BetweenExpr.NotBetween     => collectPlaceholders(e.operand()) ++ collectPlaceholders(e.low()) ++ collectPlaceholders(e.high())
        case e: InExpr.In                  => collectPlaceholders(e.operand()) ++ collectPlaceholdersInSource(e.source())
        case e: InExpr.NotIn               => collectPlaceholders(e.operand()) ++ collectPlaceholdersInSource(e.source())
        case e: CaseExpr.Case              => collectPlaceholdersBranches(e.branches().asScala.toList)
        case e: CaseExpr.CaseElse          => collectPlaceholdersBranches(e.branches().asScala.toList) ++ collectPlaceholders(e.elseBranch())
        case e: CaseExpr.SimpleCase        => collectPlaceholders(e.operand()) ++ collectPlaceholdersBranches(e.branches().asScala.toList)
        case e: CaseExpr.SimpleCaseElse    => collectPlaceholders(e.operand()) ++ collectPlaceholdersBranches(e.branches().asScala.toList) ++ collectPlaceholders(e.elseBranch())
        case e: QuantifiedExpr.All         => collectPlaceholders(e.operand())
        case e: QuantifiedExpr.Any         => collectPlaceholders(e.operand())
        case _: Exists                     => List()
        case _: Subquery.Scalar            => List()
        case f: Func                       => collectPlaceholdersFunc(f)
    }

    def collectAllPlaceholders(exprs: List[Expr]): List[Class[?]] =
        exprs.flatMap(collectPlaceholders)

    private def collectInSource(src: InExpr.InSource): List[ColumnRef] = src match {
        case vl: InExpr.ValueList     => vl.values().asScala.toList.flatMap(collectCrefs)
        case _: InExpr.SubquerySource => List()
    }

    private def collectPlaceholdersInSource(src: InExpr.InSource): List[Class[?]] = src match {
        case vl: InExpr.ValueList     => vl.values().asScala.toList.flatMap(collectPlaceholders)
        case _: InExpr.SubquerySource => List()
    }

    private def collectBranches(branches: List[CaseExpr.WhenThen]): List[ColumnRef] =
        branches.flatMap(b => collectCrefs(b.condition()) ++ collectCrefs(b.result()))

    private def collectPlaceholdersBranches(branches: List[CaseExpr.WhenThen]): List[Class[?]] =
        branches.flatMap(b => collectPlaceholders(b.condition()) ++ collectPlaceholders(b.result()))

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

    private def collectPlaceholdersFunc(f: Func): List[Class[?]] = f match {
        case e: Func.Count            => collectPlaceholders(e.expr())
        case e: Func.Sum              => collectPlaceholders(e.expr())
        case e: Func.Avg              => collectPlaceholders(e.expr())
        case e: Func.Min              => collectPlaceholders(e.expr())
        case e: Func.Max              => collectPlaceholders(e.expr())
        case e: Func.Upper            => collectPlaceholders(e.value())
        case e: Func.Lower            => collectPlaceholders(e.value())
        case e: Func.Trim             => collectPlaceholders(e.value())
        case e: Func.Length           => collectPlaceholders(e.value())
        case e: Func.Substring        => collectPlaceholders(e.value()) ++ collectPlaceholders(e.start()) ++ collectPlaceholders(e.length())
        case e: Func.Extract          => collectPlaceholders(e.source())
        case e: Func.Coalesce         => e.values().asScala.toList.flatMap(collectPlaceholders)
        case e: Func.NullIf           => collectPlaceholders(e.first()) ++ collectPlaceholders(e.second())
        case e: Func.Abs              => collectPlaceholders(e.value())
        case e: Func.Round            => collectPlaceholders(e.value()) ++ collectPlaceholders(e.precision())
        case e: Func.Floor            => collectPlaceholders(e.value())
        case e: Func.Ceil             => collectPlaceholders(e.value())
        case e: Func.Power            => collectPlaceholders(e.base()) ++ collectPlaceholders(e.exponent())
        case e: Func.Sqrt             => collectPlaceholders(e.value())
        case e: Func.Mod              => collectPlaceholders(e.left()) ++ collectPlaceholders(e.right())
        case e: Func.Cast             => collectPlaceholders(e.value())
        case _: Func.CountAll         => List()
        case _: Func.CurrentDate      => List()
        case _: Func.CurrentTimestamp => List()
    }
}