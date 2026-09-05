package io.github.hacihaciyev.fixtures;

import io.github.hacihaciyev.jdbc.Deconstruction;
import io.github.hacihaciyev.jdbc.JetQuerious;

public class DeconstructionCaller {

    private static final JetQuerious jq = JetQuerious.defaultInstance();

    public record NameEmail(String name, String email) {}

    public void updateViaDeconstruction() {
        jq.write(DeconstructionRepo.UPDATE_NAME_EMAIL, Deconstruction.dec(new NameEmail("Alice", "alice@example.com")), 1L);
    }
}
