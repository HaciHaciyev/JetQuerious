package io.github.hacihaciyev.sql;

public sealed interface JQ {
    String sql();
    
    record Read(String sql) implements JQ {
        
    }
    
    record Write(String sql) implements JQ {
        
    }
}