package io.github.hacihaciyev.jdbc;

import java.util.List;
import java.util.Optional;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.Result;

public interface WriteOperations {
    
    Result<Integer, Exception> write(JQ.Write jq, Object... args);

    <T> Result<T, Exception> writeOne(JQ.Write jq, ResultSetExtractor<T> rst, Object... params);

    <T> Result<Optional<T>, Exception> writeOption(JQ.Write jq, ResultSetExtractor<T> rst, Object... params);
    
    <T> Result<List<T>, Exception> writeMany(JQ.Write jq, ResultSetExtractor<T> rst, Object... params);
    
    Result<int[], Exception> writeBatch(JQ.Write jq, List<Object[]> batchArgs);
}