package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.sql.builders.CTEBuilder;
import io.github.hacihaciyev.sql.builders.DeleteBuilder;
import io.github.hacihaciyev.sql.builders.InsertBuilder;
import io.github.hacihaciyev.sql.builders.SelectBuilder;
import io.github.hacihaciyev.sql.builders.TransactionBuilder;
import io.github.hacihaciyev.sql.builders.UnionBuilder;
import io.github.hacihaciyev.sql.builders.UpdateBuilder;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.expressions.Expr;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.sql.value_objects.UnionType;

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
        return new CTEBuilder(name, subQuery);
    }
  
    public static UnionBuilder union(JQ.Read first, JQ.Read... rest) {
        return new UnionBuilder(UnionType.UNION, first, rest);
    }
    
    public static UnionBuilder unionAll(JQ.Read first, JQ.Read... rest) {
        return new UnionBuilder(UnionType.UNION_ALL, first, rest);
    }
    
    public static UnionBuilder intersect(JQ.Read first, JQ.Read... rest) {
        return new UnionBuilder(UnionType.INTERSECT, first, rest);
    }
    
    public static UnionBuilder except(JQ.Read first, JQ.Read... rest) {
        return new UnionBuilder(UnionType.EXCEPT, first, rest);
    }
    
    private static Expr[] toExprs(String... columns) {
        var exprs = new Expr[columns.length];
        for (var i = 0; i < columns.length; i++) exprs[i] = new ColumnRef.Base(columns[i]);
        return exprs;
    }
}