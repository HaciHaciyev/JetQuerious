package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.types.UUIDStrategy;

import java.util.UUID;

public class WrapperTypeWrongVariantCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void wrongVariant_charseqForNative() {
        jq.write(WrapperTypeRepo.UPDATE_UUID_NATIVE, new UUIDStrategy.Charseq(UUID.randomUUID()), 1L);
    }

    public void wrongVariant_binaryForCharseq() {
        jq.write(WrapperTypeRepo.UPDATE_UUID_CHARSEQ, new UUIDStrategy.Binary(UUID.randomUUID()), 1L);
    }

    public void wrongVariant_nativeForBinary() {
        jq.write(WrapperTypeRepo.UPDATE_UUID_BINARY, new UUIDStrategy.Native(UUID.randomUUID()), 1L);
    }
}