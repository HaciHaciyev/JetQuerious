package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.types.UUIDStrategy;

import java.util.UUID;

public class WrapperTypeFactoryMethodCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void createdViaTypeFactory_staticallyLooksLikeSealedInterface() {
        var strategy = UUIDStrategy.Type.NATIVE.create(UUID.randomUUID());
        jq.write(WrapperTypeRepo.UPDATE_UUID_NATIVE, strategy, 1L);
    }
}