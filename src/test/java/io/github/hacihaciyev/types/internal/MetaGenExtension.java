package io.github.hacihaciyev.types.internal;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MetaGenExtension implements BeforeAllCallback {
    
    private static volatile boolean initialized = false;
    
    @Override
    public void beforeAll(ExtensionContext context) {
        if (!initialized) {
            synchronized (MetaGenExtension.class) {
                if (!initialized) {
                    System.setProperty("jetquerious.packages", "io.github.hacihaciyev.types");
                    MetaGen.main();
                    initialized = true;
                }
            }
        }
    }
}