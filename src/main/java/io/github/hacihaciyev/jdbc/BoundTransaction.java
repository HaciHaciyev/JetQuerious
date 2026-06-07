package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.sql_error_translation.SQLErrorTranslation;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import io.github.hacihaciyev.util.Result;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BoundTransaction {
    private final Transaction    tx;
    private final DataSource     ds;
    private final List<Object[]> bindings;

    BoundTransaction(Transaction tx, DataSource ds) {
        this.tx       = requireNonNull(tx);
        this.ds       = requireNonNull(ds);
        this.bindings = new ArrayList<>();
    }

    private BoundTransaction(Transaction tx, DataSource ds, List<Object[]> bindings) {
        this.tx       = tx;
        this.ds       = ds;
        this.bindings = new ArrayList<>(bindings);
    }

    public BoundTransaction bind(Object... args) {
        requireNonNull(args, "Args cannot be null");
        var next = new BoundTransaction(tx, ds, bindings);
        next.bindings.add(args.clone());
        return next;
    }

    public Result<int[], Exception> execute() {
        try (var conn = ds.getConnection()) {
            var prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            if (tx.isolationLevel() != Transaction.IsolationLevel.DEFAULT) {
                conn.setTransactionIsolation(isolationLevel(tx.isolationLevel()));
            }

            try {
                var results  = run(conn);
                conn.commit();
                conn.setAutoCommit(prevAutoCommit);
                return new Ok<>(results);
            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(prevAutoCommit);
                return SQLErrorTranslation.handleSQLException(e);
            } catch (IllegalArgumentException e) {
                conn.rollback();
                conn.setAutoCommit(prevAutoCommit);
                return new Err<>(e);
            }
        } catch (SQLException e) {
            return SQLErrorTranslation.handleSQLException(e);
        }
    }

    private int[] run(Connection conn) throws SQLException {
        var ops      = tx.operations();
        var results  = new int[ops.length];
        var savepts  = tx.savepoints();
        var bindIdx  = 0;

        validateBindings(ops);

        var jdbcSavepoints = new java.util.HashMap<String, Savepoint>();

        for (int i = 0; i < ops.length; i++) {
            var op    = ops[i];
            var types = StatementBinder.paramTypes(op);

            for (var sp : savepts) {
                if (sp.position() == i) jdbcSavepoints.put(sp.name(), conn.setSavepoint(sp.name()));
            }

            Object[] args = types.isEmpty() ? new Object[0] : bindings.get(bindIdx++);
            results[i] = execute(conn, op, args);
        }

        for (var sp : savepts) {
            if (sp.position() == ops.length) jdbcSavepoints.put(sp.name(), conn.setSavepoint(sp.name()));
        }

        return results;
    }

    private int execute(Connection conn, JQ jq, Object[] args) throws SQLException {
        var sql  = jq.sql();
        var ctx  = switch (jq) {
            case JQ.Read(_, var c)  -> (Context) c;
            case JQ.Write(_, var c) -> (Context) c;
        };

        try (var stmt = conn.prepareStatement(sql)) {
            StatementBinder.bind(stmt, ctx, args);
            if (jq instanceof JQ.Read) {
                stmt.execute();
                return 0;
            } else {
                return stmt.executeUpdate();
            }
        }
    }

    private void validateBindings(JQ[] ops) {
        var needed = 0;
        for (var op : ops) {
            if (!StatementBinder.paramTypes(op).isEmpty()) needed++;
        }

        if (bindings.size() != needed) {
            throw new IllegalArgumentException("Expected " + needed + " bind() calls for operations with parameters, but got " + bindings.size());
        }
    }

    private static int isolationLevel(Transaction.IsolationLevel level) {
        return switch (level) {
            case READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED;
            case READ_COMMITTED   -> Connection.TRANSACTION_READ_COMMITTED;
            case REPEATABLE_READ  -> Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE     -> Connection.TRANSACTION_SERIALIZABLE;
            case DEFAULT          -> throw new IllegalStateException("DEFAULT should not reach here");
        };
    }
}