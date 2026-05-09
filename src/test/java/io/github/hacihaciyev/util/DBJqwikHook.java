package io.github.hacihaciyev.util;

import net.jqwik.api.lifecycle.BeforeContainerHook;
import net.jqwik.api.lifecycle.ContainerLifecycleContext;

public class DBJqwikHook implements BeforeContainerHook {

    @Override
    public void beforeContainer(ContainerLifecycleContext context) throws Exception {
        DBTestContainer.initialize();
    }
}