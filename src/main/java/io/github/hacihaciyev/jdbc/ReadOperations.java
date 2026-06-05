package io.github.hacihaciyev.jdbc;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.util.Result;

public interface ReadOperations {

    <T> Result<T, Exception> read(JQ jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<T, Exception> read(JQ jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
    
    <T> Result<T, Exception> read(JQ jq, Class<T> type, Object... params);
    
    <T> Result<T, Exception> read(JQ jq, Class<T> type, ResultSetType rsType, Object... params);
    
    <T> Result<Optional<T>, Exception> option(JQ jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<Optional<T>, Exception> option(JQ jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
    
    <T> Result<List<T>, Exception> list(JQ jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<List<T>, Exception> list(JQ jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
    
    <T> Result<Stream<T>, Exception> stream(JQ jq, ResultSetExtractor<T> extractor, Object... params);
    
    <T> Result<Stream<T>, Exception> stream(JQ jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params);
}