package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql_error_translation.SQLErrorTranslation;
import io.github.hacihaciyev.util.Ok;
import io.github.hacihaciyev.util.Result;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class JetQuerious implements ReadOperations, WriteOperations, Transactions {
    private final DataSource ds;

    public JetQuerious(DataSource ds) {
        this.ds = requireNonNull(ds, "DataSource cannot be null");
    }

    public static JetQuerious defaultInstance() {
        return new JetQuerious(Conf.INSTANCE.dataSource().orElseThrow(() -> new IllegalStateException("Cannot obtain datasource")));
    }

    public BoundTransaction transaction(Transaction tx) {
        return new BoundTransaction(requireNonNull(tx), ds);
    }

    @Override
    public Result<Integer, Exception> write(JQ.Write jq, Object... args) {
        return withConnection(conn -> {
            try (var stmt = prepareWrite(conn, jq, args)) {
                return new Ok<>(stmt.executeUpdate());
            }
        });
    }

    @Override
    public <T> Result<T, Exception> writeOne(JQ.Write jq, ResultSetExtractor<T> rst, Object... params) {
        return withConnection(conn -> {
            try (var stmt = prepareWrite(conn, jq, params);
                 var rs   = stmt.executeQuery()) {

                if (!rs.next()) throw new SQLException("writeOne: no rows returned from RETURNING clause");

                var result = rst.extractData(rs);
                if (result == null) throw new SQLException("writeOne: extractor returned null");

                return new Ok<>(result);
            }
        });
    }

    @Override
    public <T> Result<Optional<T>, Exception> writeOption(JQ.Write jq, ResultSetExtractor<T> rst, Object... params) {
        return withConnection(conn -> {
            try (var stmt = prepareWrite(conn, jq, params);
                 var rs   = stmt.executeQuery()) {
                     
                if (!rs.next()) return new Ok<>(Optional.empty());
                return new Ok<>(Optional.ofNullable(rst.extractData(rs)));
            }
        });
    }

    @Override
    public <T> Result<List<T>, Exception> writeMany(JQ.Write jq, ResultSetExtractor<T> rst, Object... params) {
        return withConnection(conn -> {
            try (var stmt = prepareWrite(conn, jq, params);
                 var rs   = stmt.executeQuery()) {

                var list = new ArrayList<T>();
                while (rs.next()) {
                    var row = rst.extractData(rs);
                    if (row != null) list.add(row);
                }
                return new Ok<>(List.copyOf(list));
            }
        });
    }

    @Override
    public Result<int[], Exception> writeBatch(JQ.Write jq, List<Object[]> batchArgs) {
        return withConnection(conn -> {
            var ctx = (Context) jq.context();

            try (var stmt = conn.prepareStatement(jq.sql())) {
                for (var args : batchArgs) {
                    StatementBinder.bind(stmt, ctx, args);
                    stmt.addBatch();
                }

                return new Ok<>(stmt.executeBatch());
            }
        });
    }

    @Override
    public <T> Result<T, Exception> one(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return one(jq, extractor, ResultSetType.FORWARD_ONLY_READ_ONLY, params);
    }

    @Override
    public <T> Result<T, Exception> one(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return withConnection(conn -> {
            try (var stmt = prepareRead(conn, jq, rsType, params);
                 var rs   = stmt.executeQuery()) {

                if (!rs.next()) throw new SQLException("one: no rows returned");
                
                var result = extractor.extractData(rs);
                if (result == null) throw new SQLException("one: extractor returned null");
                
                return new Ok<>(result);
            }
        });
    }

    @Override
    public <T> Result<Optional<T>, Exception> option(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return option(jq, extractor, ResultSetType.FORWARD_ONLY_READ_ONLY, params);
    }

    @Override
    public <T> Result<Optional<T>, Exception> option(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return withConnection(conn -> {
            try (var stmt = prepareRead(conn, jq, rsType, params);
                 var rs   = stmt.executeQuery()) {
                     
                if (!rs.next()) return new Ok<>(Optional.empty());
                return new Ok<>(Optional.ofNullable(extractor.extractData(rs)));
            }
        });
    }

    @Override
    public <T> Result<List<T>, Exception> many(JQ.Read jq, ResultSetExtractor<T> extractor, Object... params) {
        return many(jq, extractor, ResultSetType.FORWARD_ONLY_READ_ONLY, params);
    }

    @Override
    public <T> Result<List<T>, Exception> many(JQ.Read jq, ResultSetExtractor<T> extractor, ResultSetType rsType, Object... params) {
        return withConnection(conn -> {
            try (var stmt = prepareRead(conn, jq, rsType, params);
                var rs   = stmt.executeQuery()) {
                var list = new ArrayList<T>();
                
                while (rs.next()) {
                    var row = extractor.extractData(rs);
                    if (row != null) list.add(row);
                }
                return new Ok<>(List.copyOf(list));
            }
        });
    }

    private PreparedStatement prepareWrite(Connection conn, JQ.Write jq, Object[] args) throws SQLException {
        var ctx  = (Context) jq.context();
        var stmt = conn.prepareStatement(jq.sql());
        StatementBinder.bind(stmt, ctx, args);
        return stmt;
    }

    private PreparedStatement prepareRead(Connection conn, JQ.Read jq, ResultSetType rsType, Object[] params) throws SQLException {
        var ctx  = (Context) jq.context();
        var stmt = conn.prepareStatement(jq.sql(), rsType.type(), rsType.concurrency());
        StatementBinder.bind(stmt, ctx, params);
        return stmt;
    }

    private <T> Result<T, Exception> withConnection(ConnectionCallback<T> callback) {
        try (var conn = ds.getConnection()) {
            return callback.execute(conn);
        } catch (SQLException e) {
            return SQLErrorTranslation.handleSQLException(e);
        } catch (Exception e) {
            return new io.github.hacihaciyev.util.Err<>(e);
        }
    }

    @FunctionalInterface
    private interface ConnectionCallback<T> {
        Result<T, Exception> execute(Connection conn) throws Exception;
    }
}