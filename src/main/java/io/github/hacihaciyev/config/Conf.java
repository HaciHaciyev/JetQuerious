package io.github.hacihaciyev.config;

import io.github.hacihaciyev.types.UUIDStrategy;

import java.time.Duration;

public final class Conf {
    private final String[] packages;
    private final UUIDStrategy.Type uuidStrategy;
    private final Duration schemaTTLInSeconds;

    public static final Conf INSTANCE = new Conf();

    private Conf() {
        this.packages = defPackages();
        this.uuidStrategy = defUUIDStrategy();
        this.schemaTTLInSeconds = defSchemaCacheTTL();
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
}
