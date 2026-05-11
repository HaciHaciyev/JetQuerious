package io.github.hacihaciyev.sql.expressions;

public sealed interface ValueExpr extends Expr
    permits Func, Literal, UnaryOp, BinaryOp, 
            Exists, CaseExpr, InExpr, IsNullExpr, 
            BetweenExpr, QuantifiedExpr, Subquery.Scalar {}