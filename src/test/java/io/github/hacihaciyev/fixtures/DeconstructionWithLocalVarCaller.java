package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.Deconstruction;
import io.github.hacihaciyev.jdbc.JetQuerious;

public class DeconstructionWithLocalVarCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void updateViaDeconstruction_localVariableFirst() {
        var payload = new NameEmail("Alice", "alice@example.com");
        var dec     = Deconstruction.dec(payload);
        jq.write(DeconstructionRepo.UPDATE_NAME_EMAIL, dec, 1L);
    }
}