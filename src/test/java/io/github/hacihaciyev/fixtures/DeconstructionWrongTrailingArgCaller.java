package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.Deconstruction;
import io.github.hacihaciyev.jdbc.JetQuerious;

public class DeconstructionWrongTrailingArgCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public void updateViaDeconstruction_wrongTrailingArgType() {
        jq.write(DeconstructionRepo.UPDATE_NAME_EMAIL,
            Deconstruction.dec(new NameEmail("Alice", "alice@example.com")), "not-a-long");
    }
}