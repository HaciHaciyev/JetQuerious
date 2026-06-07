package io.github.hacihaciyev.jdbc;

import java.util.List;
import java.util.Optional;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.Result;

public interface ReadOperations {

    <T> Result<T, Exception> one(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<T, Exception> one(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
   
    <T> Result<Optional<T>, Exception> option(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<Optional<T>, Exception> option(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
    
    <T> Result<List<T>, Exception> many(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<List<T>, Exception> many(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
}