package io.github.hacihaciyev.jdbc;

public sealed interface JetQuerious {

    public static JetQuerious defaultInstance() {
        // TODO
        return new Impl();
    }
    
    record Impl() implements JetQuerious {}
}