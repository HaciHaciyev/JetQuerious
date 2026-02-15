package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.sql.internal.*;
import static io.github.hacihaciyev.sql.internal.UnionBuilder.UnionType;

public class QueryForge {

    private QueryForge() {}
    
    public static TransactionBuilder transaction() {
        return new TransactionBuilder();
    }
    
    public static SelectBuilder select(Expr... exprs) {
        return SelectBuilder.select(exprs);
    }
    
    public static SelectBuilder selectDistinct(Expr... exprs) {
        return SelectBuilder.selectDistinct(exprs);
    }
    
    public static SelectBuilder select(String... columns) {
        return SelectBuilder.select(toExprs(columns));
    }
    
    public static SelectBuilder selectDistinct(String... columns) {
        return SelectBuilder.selectDistinct(toExprs(columns));
    }
    
    public static InsertBuilder insertInto(TableRef tableRef) {
        return new InsertBuilder(tableRef);
    }
    
    public static InsertBuilder insertInto(String table) {
        return new InsertBuilder(new TableRef.Base(table));
    }

    public static UpdateBuilder update(TableRef tableRef) {
        return new UpdateBuilder(tableRef);
    }
    
    public static UpdateBuilder update(String table) {
        return new UpdateBuilder(new TableRef.Base(table));
    }

    public static DeleteBuilder deleteFrom(TableRef tableRef) {
        return new DeleteBuilder(tableRef);
    }
    
    public static DeleteBuilder deleteFrom(String table) {
        return new DeleteBuilder(new TableRef.Base(table));
    }
    
    public static CTEBuilder with(String name, JQ subQuery) {
        return new CTEBuilder(new TableRef.Base(name), subQuery);
    }
  
    public static UnionBuilder union(JQ first, JQ... rest) {
        return new UnionBuilder(UnionType.UNION, first, rest);
    }
    
    public static UnionBuilder unionAll(JQ first, JQ... rest) {
        return new UnionBuilder(UnionType.UNION_ALL, first, rest);
    }
    
    public static UnionBuilder intersect(JQ first, JQ... rest) {
        return new UnionBuilder(UnionType.INTERSECT, first, rest);
    }
    
    public static UnionBuilder except(JQ first, JQ... rest) {
        return new UnionBuilder(UnionType.EXCEPT, first, rest);
    }
    
    private static Expr[] toExprs(String... columns) {
        var exprs = new Expr[columns.length];
        for (var i = 0; i < columns.length; i++) exprs[i] = new Expr.Col(columns[i]);
        return exprs;
    }
}