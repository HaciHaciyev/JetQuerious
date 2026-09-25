package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.types.AsObject;
import io.github.hacihaciyev.types.AsString;

public class WrapperTypeWrongWrapperCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void wrongWrapper_asObjectForAsString() {
        jq.write(WrapperTypeRepo.UPDATE_AS_STRING, new AsObject("value"), 1L);
    }

    public void wrongWrapper_asStringForAsObject() {
        jq.write(WrapperTypeRepo.UPDATE_AS_OBJECT, new AsString("value"), 1L);
    }

    public void wrongWrapper_rawStringForAsString() {
        jq.write(WrapperTypeRepo.UPDATE_AS_STRING, "not-wrapped", 1L);
    }
}