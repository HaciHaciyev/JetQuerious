package io.github.hacihaciyev.sql.internal;

import io.github.hacihaciyev.sql.JQ;

public class UnionBuilder {
    
    public enum UnionType {
        UNION,
        UNION_ALL,
        INTERSECT,
        EXCEPT
    }

    public UnionBuilder(UnionType type, JQ first, JQ... rest) {
        
    }
}