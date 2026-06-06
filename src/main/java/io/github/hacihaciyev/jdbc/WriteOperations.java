package io.github.hacihaciyev.jdbc;

import java.util.List;
import java.util.stream.Stream;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.Result;

public interface WriteOperations {
    
    Result<Integer, Exception> write(JQ.Write jq, Object... args);

    <T> Result<T, Exception> writeOne(JQ.Write jq, ResultSetExtractor<T> rst, Object... params);
    
    <T> Result<List<T>, Exception> writeMany(JQ.Write jq, ResultSetExtractor<T> rst, Object... params);

    <T> Result<Stream<T>, Exception> writeStream(JQ.Write jq, ResultSetExtractor<T> rst, Object... params);
    
    Result<Integer, Exception> writeArray(JQ.Write jq, String arrayDef, Object[] array, Object... args);

    Result<int[], Exception> writeBatch(JQ.Write jq, List<Object[]> batchArgs);
}