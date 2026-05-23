package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.sql.internal.builders.CTESQL;
import io.github.hacihaciyev.sql.internal.value_objects.CTEEntry;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class CTEBuilder {
    private final List<CTEEntry> entries;
    
    private CTEBuilder(List<CTEEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public CTEBuilder(String name, JQ query) {
        requireNonNull(name,  "CTE name cannot be null");
        requireNonNull(query, "CTE query cannot be null");
        if (name.isBlank()) throw new IllegalArgumentException("CTE name cannot be blank");

        var entries = new ArrayList<CTEEntry>();
        entries.add(new CTEEntry.Regular(name, query));
        
        this.entries = List.copyOf(entries);
    }

    public CTEBuilder with(String name, JQ query) {
        requireNonNull(name,  "CTE name cannot be null");
        requireNonNull(query, "CTE query cannot be null");
        if (name.isBlank()) throw new IllegalArgumentException("CTE name cannot be blank");

        var entries = new ArrayList<>(this.entries);
        entries.add(new CTEEntry.Regular(name, query));
        return new CTEBuilder(entries);
    }

    public CTEBuilder withRecursive(String name, JQ.Read base, JQ.Read recursive) {
        return withRecursive(name, base, recursive, UnionType.UNION_ALL);
    }

    public CTEBuilder withRecursive(String name, JQ.Read base, JQ.Read recursive, UnionType unionType) {
        requireNonNull(name,      "CTE name cannot be null");
        requireNonNull(base,      "Base query cannot be null");
        requireNonNull(recursive, "Recursive query cannot be null");
        requireNonNull(unionType, "Union type cannot be null");
        if (name.isBlank()) throw new IllegalArgumentException("CTE name cannot be blank");

        var entries = new ArrayList<CTEEntry>(this.entries);
        entries.add(new CTEEntry.Recursive(name, base, recursive, unionType));
        return new CTEBuilder(entries);
    }

    public JQ.Read build(JQ.Read finalQuery) {
        requireNonNull(finalQuery, "Final query cannot be null");
        var sql = CTESQL.build(entries, finalQuery);
        return new JQ.Read(sql, finalQuery.context());
    }

    public JQ.Write build(JQ.Write finalQuery) {
        requireNonNull(finalQuery, "Final query cannot be null");
        var sql = CTESQL.build(entries, finalQuery);
        return new JQ.Write(sql, finalQuery.context());
    }
}
