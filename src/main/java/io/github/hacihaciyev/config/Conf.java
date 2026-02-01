package io.github.hacihaciyev.config;

import io.github.hacihaciyev.types.UUIDStrategy;

public final class Conf {
    private final String[] packages;
    private final UUIDStrategy.Type uuidStrategy;

    public static final Conf INSTANCE = new Conf();

    private Conf() {
        this.packages = defPackages();
        this.uuidStrategy = defUUIDStrategy();
    }

    public String[] packages() {
        return packages.clone();
    }

    public UUIDStrategy.Type uuidStrategy() {
        return uuidStrategy;
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
}
