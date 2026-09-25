package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.types.AsObject;
import io.github.hacihaciyev.types.AsString;
import io.github.hacihaciyev.types.UUIDStrategy;

import java.util.UUID;

public class WrapperTypeCorrectCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void insertWithUuidNative() {
        jq.write(WrapperTypeRepo.INSERT_WITH_UUID_NATIVE, 1L, "Alice", new UUIDStrategy.Native(UUID.randomUUID()));
    }

    public void updateWithUuidNative() {
        jq.write(WrapperTypeRepo.UPDATE_UUID_NATIVE, new UUIDStrategy.Native(UUID.randomUUID()), 1L);
    }

    public void updateWithUuidCharseq() {
        jq.write(WrapperTypeRepo.UPDATE_UUID_CHARSEQ, new UUIDStrategy.Charseq(UUID.randomUUID()), 1L);
    }

    public void updateWithUuidBinary() {
        jq.write(WrapperTypeRepo.UPDATE_UUID_BINARY, new UUIDStrategy.Binary(UUID.randomUUID()), 1L);
    }

    public void updateWithAsString() {
        jq.write(WrapperTypeRepo.UPDATE_AS_STRING, new AsString("some-value"), 1L);
    }

    public void updateWithAsObject() {
        jq.write(WrapperTypeRepo.UPDATE_AS_OBJECT, new AsObject(42), 1L);
    }
}