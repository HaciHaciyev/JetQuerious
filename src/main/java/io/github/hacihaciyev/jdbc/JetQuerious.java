package io.github.hacihaciyev.jdbc;

public sealed interface JetQuerious extends ReadOperations, WriteOperations, Transactions {

    public static JetQuerious defaultInstance() {
        // TODO
        return new Impl();
    }
    
    record Impl() implements JetQuerious {}
}