package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.JQ.Read;
import io.github.hacihaciyev.sql.JQ.Write;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.value_objects.TableRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class CTEBuilder {
    
    private final List<CTEPart> parts = new ArrayList<>();
    
    record CTEPart(String name, JQ query) {
        public CTEPart {
            requireNonNull(name, "CTE name cannot be null");
            requireNonNull(query, "CTE query cannot be null");
            if (name.trim().isBlank()) throw new IllegalArgumentException("CTE name cannot be blank");
            name = name.trim();
        }
    }
    
    public CTEBuilder(String name, JQ query) {
        parts.add(new CTEPart(name, query));
    }
    
    public CTEBuilder with(String name, JQ query) {
        for (var part : parts) {
            if (part.name.equalsIgnoreCase(name)) throw new IllegalArgumentException("Duplicate CTE name: " + name);
        }
        
        parts.add(new CTEPart(name, query));
        return this;
    }
    
    // TODO contect for validation
    public JQ.Read read(JQ mainQuery) throws SchemaVerificationException {
        requireNonNull(mainQuery, "Main query cannot be null");
        
        var sql = buildSql(mainQuery);
        var tableRefs = collectTableRefs(mainQuery);
        var columnRefs = collectColumnRefs(mainQuery);
        
        return new JQ.Read(sql, tableRefs, columnRefs);
    }
    
    // TODO context for validation
    public JQ.Write write(JQ mainQuery) throws SchemaVerificationException {
        requireNonNull(mainQuery, "Main query cannot be null");
        
        var sql = buildSql(mainQuery);
        var tableRefs = collectTableRefs(mainQuery);
        var columnRefs = collectColumnRefs(mainQuery);
        
        return new JQ.Write(sql, tableRefs, columnRefs);
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