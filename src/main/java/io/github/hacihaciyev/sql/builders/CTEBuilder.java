package io.github.hacihaciyev.sql.builders;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.value_objects.UnionType;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql.internal.ContextFactory;
import io.github.hacihaciyev.sql.internal.DML;
import io.github.hacihaciyev.sql.internal.DQL;
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
        var sql     = CTESQL.build(entries, finalQuery);
        var context = ContextFactory.cteReadContext(entryContexts(), (Context) finalQuery.context());
        return new JQ.Read(sql, (DQL) context);
    }

    public JQ.Write build(JQ.Write finalQuery) {
        requireNonNull(finalQuery, "Final query cannot be null");
        var sql     = CTESQL.build(entries, finalQuery);
        var context = ContextFactory.cteWriteContext(entryContexts(), (Context) finalQuery.context());
        return new JQ.Write(sql, (DML) context);
    }

    private List<Context> entryContexts() {
        var contexts = new ArrayList<Context>();
        for (var entry : entries) {
            switch (entry) {
                case CTEEntry.Regular r -> contexts.add(context(r.query()));
                case CTEEntry.Recursive rec -> {
                    contexts.add(context(rec.base()));
                    contexts.add(context(rec.recursive()));
                }
                default -> throw new IllegalArgumentException("Unknown CTE entry type: " + entry.getClass());
            }
        }
        return contexts;
    }

    private Context context(JQ jq) {
        return switch (jq) {
            case JQ.Read read -> (Context) read.context();
            case JQ.Write write -> (Context) write.context();
        };
    }

    private void checkDuplicate(String name) {
        var trimmed = name.trim();
        for (var entry : entries) {
            if (entry.name().equalsIgnoreCase(trimmed)) throw new IllegalArgumentException("Duplicate CTE name: " + trimmed);
        }
    }
}