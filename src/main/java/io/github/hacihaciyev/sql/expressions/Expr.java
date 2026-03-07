package io.github.hacihaciyev.sql.expressions;

public sealed interface Expr 
        permits ColumnRef, Func, Literal, UnaryOp, BinaryOp, 
        Exists, CaseExpr, InExpr, Subquery,
        IsNullExpr, BetweenExpr, QuantifiedExpr {}