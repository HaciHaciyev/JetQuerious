package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.JQ.Read;
import io.github.hacihaciyev.sql.expressions.ColumnRef;
import io.github.hacihaciyev.sql.value_objects.Limit;
import io.github.hacihaciyev.sql.value_objects.Offset;
import io.github.hacihaciyev.sql.value_objects.TableRef;
import io.github.hacihaciyev.sql.value_objects.UnionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class UnionBuilder {
    
    private final List<UnionPart> parts = new ArrayList<>();
    
    record UnionPart(UnionType type, JQ.Read query) {
        public UnionPart {
            requireNonNull(type, "Union type cannot be null");
            requireNonNull(query, "JQ cannot be null");
        }
    }
    
    public UnionBuilder(UnionType type, JQ.Read first, JQ.Read... rest) {
        requireNonNull(rest, "Rest queries cannot be null");
        if (rest.length == 0) throw new IllegalArgumentException("Union requires at least two queries");
        
        parts.add(new UnionPart(type, first));
        for (var q : rest) parts.add(new UnionPart(type, q));
    }
    
    public UnionBuilder union(JQ.Read query) {
        parts.add(new UnionPart(UnionType.UNION, query));
        return this;
    }
    
    public UnionBuilder unionAll(JQ.Read query) {
        parts.add(new UnionPart(UnionType.UNION_ALL, query));
        return this;
    }
    
    public UnionBuilder intersect(JQ.Read query) {
        parts.add(new UnionPart(UnionType.INTERSECT, query));
        return this;
    }
    
    public UnionBuilder except(JQ.Read query) {
        parts.add(new UnionPart(UnionType.EXCEPT, query));
        return this;
    }
    
    public OrderByStage orderBy(ColumnRef... columns) {
        return new OrderByStage(parts, List.of(columns));
    }
    
    public OrderByStage orderBy(String... columns) {
        return orderBy(Arrays.stream(columns).map(ColumnRef.Base::new).toArray(ColumnRef[]::new));
    }
    
    public LimitStage limit(Limit limit) {
        return new LimitStage(parts, List.of(), limit);
    }
    
    public JQ.Read build() throws SchemaVerificationException {
        return buildQuery(parts, null, null, null);
    }
    
    public record OrderByStage(List<UnionPart> parts, List<ColumnRef> orderByColumns) {
        public OrderByStage {
            parts = List.copyOf(requireNonNull(parts));
            orderByColumns = List.copyOf(requireNonNull(orderByColumns));
            
            for (var part : parts) requireNonNull(part, "Union part cannot be null");
            for (var cr : orderByColumns) requireNonNull(cr, "Order by column cannot be null");
        }
        
        public LimitStage limit(Limit limit) {
            return new LimitStage(parts, orderByColumns, limit);
        }
        
        public JQ.Read build() throws SchemaVerificationException {
            return buildQuery(parts, orderByColumns, null, null);
        }
    }
    
    public record LimitStage(List<UnionPart> parts, List<ColumnRef> orderByColumns, Limit limit) {
        public LimitStage {
            parts = List.copyOf(requireNonNull(parts));
            orderByColumns = List.copyOf(requireNonNull(orderByColumns));
            
            for (var part : parts) requireNonNull(part, "Union part cannot be null");
            for (var cr : orderByColumns) requireNonNull(cr, "Order by column cannot be null");
            
            requireNonNull(limit);
        }
        
        public JQ.Read offset(Offset offset) throws SchemaVerificationException {
            requireNonNull(offset, "Offset cannot be null");
            return buildQuery(parts, orderByColumns, limit, offset);
        }
        
        public JQ.Read build() throws SchemaVerificationException {
            return buildQuery(parts, orderByColumns, limit, null);
        }
    }
    
    private static JQ.Read buildQuery(List<UnionPart> parts, List<ColumnRef> orderByColumns, Limit limit, Offset offset)
        throws SchemaVerificationException {
                                          
        var sql = buildSql(parts, orderByColumns, limit, offset);
        var tableRefs = collectTableRefs(parts);
        var columnRefs = collectColumnRefs(parts, orderByColumns);
        
        return new JQ.Read(sql, tableRefs, columnRefs);
    }
    
    private static String buildSql(List<UnionPart> parts, List<ColumnRef> orderByColumns, Limit limit, Offset offset) {
        var sb = new StringBuilder("(").append(parts.get(0).query.sql()).append(")");
        
        for (int i = 1; i < parts.size(); i++) {
            var part = parts.get(i);
            
            var op = switch (part.type) {
                case UNION -> " UNION ";
                case UNION_ALL -> " UNION ALL ";
                case INTERSECT -> " INTERSECT ";
                case EXCEPT -> " EXCEPT ";
            };
            
            sb.append(op).append("(").append(part.query.sql()).append(")");
        }
        
        if (orderByColumns != null && !orderByColumns.isEmpty()) {
            sb.append(" ORDER BY ");
            sb.append(String.join(", ", orderByColumns.stream().map(ColumnRef::toString).toList()));
        }
        
        if (limit != null) sb.append(" LIMIT ").append(limit);
        if (offset != null) sb.append(" OFFSET ").append(offset);
        
        return sb.toString();
    }
    
    private static TableRef[] collectTableRefs(List<UnionPart> parts) {
        var allRefs = new ArrayList<TableRef>();
        for (var part : parts) allRefs.addAll(Arrays.asList(part.query.tableRefs()));
        return allRefs.toArray(new TableRef[0]);
    }
    
    private static ColumnRef[] collectColumnRefs(List<UnionPart> parts, List<ColumnRef> orderByColumns) {
        var allRefs = new ArrayList<ColumnRef>();
        for (var part : parts) allRefs.addAll(Arrays.asList(part.query.columnRefs()));
        if (orderByColumns != null) allRefs.addAll(orderByColumns);
        return allRefs.toArray(new ColumnRef[0]);
    }
}