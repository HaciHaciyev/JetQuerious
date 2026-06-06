package io.github.hacihaciyev.jdbc;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.util.Result;

public class JetQuerious implements ReadOperations, WriteOperations, Transactions {
    private final DataSource ds;
    
    public JetQuerious(DataSource ds) {
        this.ds = ds;
    }
    
    public static JetQuerious defaultInstance() {
        return new JetQuerious(Conf.INSTANCE.dataSource().orElseThrow(() -> new IllegalStateException("Cannot obtain datasource")));
    }
    
    public Result<int[], Exception> transaction(Transaction transaction, Object... args) {
        return null;
    }
    
    public Result<Integer, Exception> write(JQ.Write jq, Object... args) {
        return null;
    }

    public <T> Result<T, Exception> writeOne(JQ.Write jq, ResultSetExtractor<T> rst, Object... params) {
        return null;
    }
    
    public <T> Result<List<T>, Exception> writeMany(JQ.Write jq, ResultSetExtractor<T> rst, Object... params) {
        return null;
    }

    public <T> Result<Stream<T>, Exception> writeStream(JQ.Write jq, ResultSetExtractor<T> rst, Object... params) {
        return null;
    }
    
    public Result<Integer, Exception> writeArray(JQ.Write jq, String arrayDef, Object[] array, Object... args) {
        return null;
    }

    public Result<int[], Exception> writeBatch(JQ.Write jq, List<Object[]> batchArgs) {
        return null;
    }
    
    public <T> Result<T, Exception> read(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return null;
    }
    
    public <T> Result<T, Exception> read(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return null;
    }
    
    public <T> Result<T, Exception> read(JQ.Read jq, Class<T> type, Object... params) {
        return null;
    }
    
    public <T> Result<T, Exception> read(JQ.Read jq, Class<T> type, ResultSetType rsType, Object... params) {
        return null;
    }
    
    public <T> Result<Optional<T>, Exception> option(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return null;
    }
    
    public <T> Result<Optional<T>, Exception> option(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return null;
    }
    
    public <T> Result<List<T>, Exception> list(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return null;
    }
    
    public <T> Result<List<T>, Exception> list(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return null;
    }
    
    public <T> Result<Stream<T>, Exception> stream(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return null;
    }
    
    public <T> Result<Stream<T>, Exception> stream(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return null;
    }
}