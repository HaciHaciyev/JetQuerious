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
        var entries = new ArrayList<CTEEntry>();
        entries.add(new CTEEntry.Regular(name, query));

        this.entries = List.copyOf(entries);
    }

    public CTEBuilder with(String name, JQ query) {
        var entry = new CTEEntry.Regular(name, query);
        checkDuplicate(name);

        var entries = new ArrayList<>(this.entries);
        entries.add(entry);
        return new CTEBuilder(entries);
    }

    public CTEBuilder withRecursive(String name, JQ.Read base, JQ.Read recursive) {
        return withRecursive(name, base, recursive, UnionType.UNION_ALL);
    }

    public CTEBuilder withRecursive(String name, JQ.Read base, JQ.Read recursive, UnionType unionType) {
        var entry = new CTEEntry.Recursive(name, base, recursive, unionType);
        checkDuplicate(name);

        var entries = new ArrayList<>(this.entries);
        entries.add(entry);
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

    private void checkDuplicate(String name) {
        var trimmed = name.trim();
        for (var entry : entries) {
            if (entry.name().equalsIgnoreCase(trimmed)) throw new IllegalArgumentException("Duplicate CTE name: " + trimmed);
        }
    }
}