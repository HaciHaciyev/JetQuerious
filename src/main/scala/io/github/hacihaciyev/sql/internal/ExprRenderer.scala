package main.scala.io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.sql.expressions.*

import scala.jdk.CollectionConverters.*

object ExprRenderer {

    def render(expr: Expr): String = expr match {
        case col: ColumnRef                  => renderColumnRef(col)
        case op: BinaryOp                    => s"(${render(op.left())} ${renderBinaryOp(op.operator())} ${render(op.right())})"
        case op: UnaryOp                     => s"${renderUnaryOp(op.operator())}${render(op.expr())}"
        case e: IsNullExpr.IsNull            => s"${render(e.operand())} IS NULL"
        case e: IsNullExpr.IsNotNull         => s"${render(e.operand())} IS NOT NULL"
        case e: BetweenExpr.Between          => s"${render(e.operand())} BETWEEN ${render(e.low())} AND ${render(e.high())}"
        case e: BetweenExpr.NotBetween       => s"${render(e.operand())} NOT BETWEEN ${render(e.low())} AND ${render(e.high())}"
        case e: InExpr.In                    => s"${render(e.operand())} IN ${renderInSource(e.source())}"
        case e: InExpr.NotIn                 => s"${render(e.operand())} NOT IN ${renderInSource(e.source())}"
        case e: CaseExpr.Case                => s"CASE ${renderBranches(e.branches().asScala.toList)} END"
        case e: CaseExpr.CaseElse            => s"CASE ${renderBranches(e.branches().asScala.toList)} ELSE ${render(e.elseBranch())} END"
        case e: CaseExpr.SimpleCase          => s"CASE ${render(e.operand())} ${renderBranches(e.branches().asScala.toList)} END"
        case e: CaseExpr.SimpleCaseElse      => s"CASE ${render(e.operand())} ${renderBranches(e.branches().asScala.toList)} ELSE ${render(e.elseBranch())} END"
        case e: QuantifiedExpr.All           => s"${render(e.operand())} ${renderComparison(e.operator())} ALL (${e.subquery().jq().sql()})"
        case e: QuantifiedExpr.Any           => s"${render(e.operand())} ${renderComparison(e.operator())} ANY (${e.subquery().jq().sql()})"
        case e: Exists                       => s"EXISTS (${e.subquery().jq().sql()})"
        case e: Subquery.ScalarSubquery      => s"(${e.jq().sql()})"
        case f: Func                         => renderFunc(f)
        case l: Literal                      => renderLiteral(l)
    }

    private def renderColumnRef(col: ColumnRef): String = col match {
        case c: ColumnRef.Base         => c.name()
        case c: ColumnRef.Alias        => s"${c.name()} AS ${c.alias()}"
        case c: ColumnRef.VariableBase => s"${c.variable()}.${c.name()}"
        case c: ColumnRef.VariableAlias => s"${c.variable()}.${c.name()} AS ${c.alias()}"
    }

    private def renderBinaryOp(op: BinaryOp.BinaryOperator): String = op match {
        case BinaryOp.BinaryOperator.PLUS     => "+"
        case BinaryOp.BinaryOperator.MINUS    => "-"
        case BinaryOp.BinaryOperator.MULTIPLY => "*"
        case BinaryOp.BinaryOperator.DIV      => "/"
        case BinaryOp.BinaryOperator.AND      => "AND"
        case BinaryOp.BinaryOperator.OR       => "OR"
        case BinaryOp.BinaryOperator.EQ       => "="
        case BinaryOp.BinaryOperator.NEQ      => "<>"
        case BinaryOp.BinaryOperator.GT       => ">"
        case BinaryOp.BinaryOperator.GTE      => ">="
        case BinaryOp.BinaryOperator.LT       => "<"
        case BinaryOp.BinaryOperator.LTE      => "<="
        case BinaryOp.BinaryOperator.LIKE     => "LIKE"
        case BinaryOp.BinaryOperator.NOT_LIKE => "NOT LIKE"
    }

    private def renderUnaryOp(op: UnaryOp.UnaryOperator): String = op match {
        case UnaryOp.UnaryOperator.PLUS  => "+"
        case UnaryOp.UnaryOperator.MINUS => "-"
        case UnaryOp.UnaryOperator.NOT   => "NOT "
    }

    private def renderComparison(op: QuantifiedExpr.ComparisonOperator): String = op match {
        case QuantifiedExpr.ComparisonOperator.EQ  => "="
        case QuantifiedExpr.ComparisonOperator.NEQ => "<>"
        case QuantifiedExpr.ComparisonOperator.GT  => ">"
        case QuantifiedExpr.ComparisonOperator.GTE => ">="
        case QuantifiedExpr.ComparisonOperator.LT  => "<"
        case QuantifiedExpr.ComparisonOperator.LTE => "<="
    }

    private def renderInSource(src: InExpr.InSource): String = src match {
        case vl: InExpr.ValueList      => s"(${vl.values().asScala.map(render).mkString(", ")})"
        case sq: InExpr.SubquerySource => s"(${sq.subquery().jq().sql()})"
    }

    private def renderBranches(branches: List[CaseExpr.WhenThen]): String =
        branches.map(b => s"WHEN ${render(b.condition())} THEN ${render(b.result())}").mkString(" ")

    private def renderLiteral(l: Literal): String = l match {
        case v: Literal.StringLiteral     => s"'${v.value().replace("'", "''")}'"
        case v: Literal.IntLiteral        => v.value().toString
        case v: Literal.LongLiteral       => v.value().toString
        case v: Literal.BooleanLiteral    => v.value().toString.toUpperCase
        case _: Literal.NullLiteral       => "NULL"
        case v: Literal.FloatLiteral      => v.value().toString
        case v: Literal.DoubleLiteral     => v.value().toString
        case v: Literal.BigDecimalLiteral => v.value().toPlainString
        case v: Literal.GenericLiteral    => v.value().toString
    }

    private def renderFunc(f: Func): String = f match {
        case e: Func.Count            => s"COUNT(${if e.distinct() then "DISTINCT " else ""}${render(e.expr())})"
        case _: Func.CountAll         => "COUNT(*)"
        case e: Func.Sum              => s"SUM(${if e.distinct() then "DISTINCT " else ""}${render(e.expr())})"
        case e: Func.Avg              => s"AVG(${if e.distinct() then "DISTINCT " else ""}${render(e.expr())})"
        case e: Func.Min              => s"MIN(${render(e.expr())})"
        case e: Func.Max              => s"MAX(${render(e.expr())})"
        case e: Func.Upper            => s"UPPER(${render(e.value())})"
        case e: Func.Lower            => s"LOWER(${render(e.value())})"
        case e: Func.Trim             => s"TRIM(${render(e.value())})"
        case e: Func.Length           => s"LENGTH(${render(e.value())})"
        case e: Func.Substring        => s"SUBSTRING(${render(e.value())} FROM ${render(e.start())} FOR ${render(e.length())})"
        case _: Func.CurrentDate      => "CURRENT_DATE"
        case _: Func.CurrentTimestamp => "CURRENT_TIMESTAMP"
        case e: Func.Extract          => s"EXTRACT(${e.field()} FROM ${render(e.source())})"
        case e: Func.Coalesce         => s"COALESCE(${e.values().asScala.map(render).mkString(", ")})"
        case e: Func.NullIf           => s"NULLIF(${render(e.first())}, ${render(e.second())})"
        case e: Func.Abs              => s"ABS(${render(e.value())})"
        case e: Func.Round            => s"ROUND(${render(e.value())}, ${render(e.precision())})"
        case e: Func.Floor            => s"FLOOR(${render(e.value())})"
        case e: Func.Ceil             => s"CEIL(${render(e.value())})"
        case e: Func.Power            => s"POWER(${render(e.base())}, ${render(e.exponent())})"
        case e: Func.Sqrt             => s"SQRT(${render(e.value())})"
        case e: Func.Mod              => s"MOD(${render(e.left())}, ${render(e.right())})"
        case e: Func.Cast             => s"CAST(${render(e.value())} AS ${e.targetType()})"
    }
}