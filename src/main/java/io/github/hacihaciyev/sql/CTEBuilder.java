package io.github.hacihaciyev.sql;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class CTEBuilder {
    
    private final List<CTEPart> parts = new ArrayList<>();
    
    record CTEPart(TableRef.Base name, JQ query) {}
    
    public CTEBuilder(TableRef.Base name, JQ query) {
        requireNonNull(name, "CTE name cannot be null");
        requireNonNull(query, "CTE query cannot be null");
        parts.add(new CTEPart(name, query));
    }
    
    public CTEBuilder with(TableRef.Base name, JQ query) {
        requireNonNull(name, "CTE name cannot be null");
        requireNonNull(query, "CTE query cannot be null");
        parts.add(new CTEPart(name, query));
        return this;
    }
    
    public JQ.Read read(JQ mainQuery) throws SchemaVerificationException {
        requireNonNull(mainQuery, "Main query cannot be null");
        
        var sql = buildSql(mainQuery);
        var tableRefs = collectTableRefs(mainQuery);
        var columnRefs = collectColumnRefs(mainQuery);
        
        return JQ.Read.withoutValidation(sql, tableRefs, columnRefs);
    }
    
    public JQ.Write write(JQ mainQuery) throws SchemaVerificationException {
        requireNonNull(mainQuery, "Main query cannot be null");
        
        var sql = buildSql(mainQuery);
        var tableRefs = collectTableRefs(mainQuery);
        var columnRefs = collectColumnRefs(mainQuery);
        
        return JQ.Write.withoutValidation(sql, tableRefs, columnRefs);
    }
    
    private String buildSql(JQ mainQuery) {
        var sb = new StringBuilder("WITH ");
        
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            if (i > 0) sb.append(", ");
            
            sb.append(part.name)
              .append(" AS (")
              .append(part.query.sql())
              .append(")");
        }
        
        sb.append(" ").append(mainQuery.sql());
        return sb.toString();
    }
    
    private TableRef[] collectTableRefs(JQ mainQuery) {
        var allRefs = new ArrayList<TableRef>();        
        for (var part : parts) allRefs.addAll(Arrays.asList(part.query.tableRefs()));
        allRefs.addAll(Arrays.asList(mainQuery.tableRefs()));
        return allRefs.toArray(new TableRef[0]);
    }
    
    private ColumnRef[] collectColumnRefs(JQ mainQuery) {
        var allRefs = new ArrayList<ColumnRef>();
        for (var part : parts) allRefs.addAll(Arrays.asList(part.query.columnRefs()));
        allRefs.addAll(Arrays.asList(mainQuery.columnRefs()));
        return allRefs.toArray(new ColumnRef[0]);
    }
}