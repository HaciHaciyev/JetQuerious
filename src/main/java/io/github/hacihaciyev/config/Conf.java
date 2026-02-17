package io.github.hacihaciyev.config;

import io.github.hacihaciyev.types.UUIDStrategy;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public final class Conf {
    private final String[] packages;
    private final UUIDStrategy.Type uuidStrategy;
    private final Duration schemaTTLInSeconds;
    private final int schemaCacheSize;
    private final AtomicReference<DataSource> dataSourceRef = new AtomicReference<>();

    public static final Conf INSTANCE = new Conf();

    private Conf() {
        this.packages = defPackages();
        this.uuidStrategy = defUUIDStrategy();
        this.schemaTTLInSeconds = defSchemaCacheTTL();
        this.schemaCacheSize = defSchemaCacheSize();
    }

    public String[] packages() {
        return packages.clone();
    }

    public UUIDStrategy.Type uuidStrategy() {
        return uuidStrategy;
    }

    public Duration schemaTTLInSeconds() {
        return schemaTTLInSeconds;
    }

    public int schemaCacheSize() {
        return schemaCacheSize;
    }

    public Optional<DataSource> dataSource() {
        return Optional.ofNullable(dataSourceRef.get());
    }

    public void defDataSource(DataSource dataSource) {
        requireNonNull(dataSource, "DataSource cannot be null");
        dataSourceRef.set(dataSource);
    }

    private UUIDStrategy.Type defUUIDStrategy() {
        try {
            return UUIDStrategy.Type.valueOf(System.getProperty("jetquerious.uuid_strategy"));
        } catch (Exception _) {
            return UUIDStrategy.Type.NATIVE;
        }
    }

    private static String[] defPackages() {
        var pkgs = System.getProperty("jetquerious.packages");
        if (pkgs != null && !pkgs.isBlank()) return pkgs.split(";");
        return new String[0];
    }

    private Duration defSchemaCacheTTL() {
        var ttl = System.getProperty("jetquerious.schema.cache.ttl");
        if (ttl == null) return Duration.ofSeconds(5);
        try {
            return Duration.parse(ttl);
        } catch (Exception _) {
            return Duration.ofSeconds(5);
        }
    }

    private int defSchemaCacheSize() {
        try {
            var cacheSize = Integer.parseInt(System.getProperty("jetquerious.schema.cache.size"));
            if (cacheSize <= 0) return 128;
            return Integer.highestOneBit(cacheSize - 1) << 1;
        } catch (Exception _) {
            return 128;
        }
    }
}
