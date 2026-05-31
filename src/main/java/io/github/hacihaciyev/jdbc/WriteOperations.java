package io.github.hacihaciyev.jdbc;

import java.util.List;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.Result;

public interface WriteOperations {

    Result<Integer, Exception> write(JQ jq, Object... args);
    
    Result<Integer, Exception> writeArrayOf(JQ jq, String arrayDef, Object[] array, Object... args);
    
    Result<int[], Exception> writeBatch(JQ jq, List<Object[]> batchArgs);
}