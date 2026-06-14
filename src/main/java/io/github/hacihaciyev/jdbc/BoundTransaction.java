package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.sql.internal.Context;
import io.github.hacihaciyev.sql_error_translation.SQLErrorTranslation;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;
import io.github.hacihaciyev.util.Result;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
        var ops    = tx.operations();
        var needed = countNeeded(ops);

        if (bindings.size() != needed) {
            return new Err<>(new IllegalArgumentException("Expected " + needed + " bind() calls but got " + bindings.size()));
        }

        Connection conn           = null;
        boolean    prevAutoCommit = true;
        
        try {
            conn           = ds.getConnection();
            prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            if (tx.isolationLevel() != Transaction.IsolationLevel.DEFAULT) conn.setTransactionIsolation(isolationLevel(tx.isolationLevel()));

            var results = run(conn, ops);
            conn.commit();
            conn.setAutoCommit(prevAutoCommit);
            return new Ok<>(results);
        } catch (SQLException e) {
            rollback(conn, prevAutoCommit);
            return SQLErrorTranslation.handleSQLException(e);
        } catch (IllegalArgumentException e) {
            rollback(conn, prevAutoCommit);
            return new Err<>(e);
        } finally {
            closeQuietly(conn);
        }
    }

    private int[] run(Connection conn, JQ[] ops) throws SQLException {
        var results    = new int[ops.length];
        var savepts    = tx.savepoints();
        var bindIdx    = 0;
        var jdbcSpts   = new HashMap<String, Savepoint>();

        for (int i = 0; i < ops.length; i++) {
            for (var sp : savepts) {
                if (sp.position() == i) jdbcSpts.put(sp.name(), conn.setSavepoint(sp.name()));
            }

            var op    = ops[i];
            var types = StatementBinder.paramTypes(op);
            var args  = types.isEmpty() ? new Object[0] : bindings.get(bindIdx++);

            results[i] = executeOp(conn, op, args);
        }

        for (var sp : savepts) {
            if (sp.position() == ops.length) jdbcSpts.put(sp.name(), conn.setSavepoint(sp.name()));
        }

        return results;
    }

    private int executeOp(Connection conn, JQ jq, Object[] args) throws SQLException {
        var ctx = switch (jq) {
            case JQ.Read(_, var c)  -> (Context) c;
            case JQ.Write(_, var c) -> (Context) c;
        };

        try (var stmt = conn.prepareStatement(jq.sql())) {
            StatementBinder.bind(stmt, ctx, args);
            if (jq instanceof JQ.Read) {
                stmt.execute();
                return 0;
            }
            return stmt.executeUpdate();
        }
    }

    private int countNeeded(JQ[] ops) {
        var count = 0;
        for (var op : ops) {
            if (!StatementBinder.paramTypes(op).isEmpty()) count++;
        }
        return count;
    }

    private static void rollback(Connection conn, boolean prevAutoCommit) {
        if (conn == null) return;

        try { 
            conn.rollback();
        } catch (SQLException _) {}
       
        try {
            conn.setAutoCommit(prevAutoCommit);
        } catch (SQLException _) {}
    }

    private static void closeQuietly(Connection conn) {
        if (conn == null) return;

        try { 
            conn.close();
        } catch (SQLException _) {}
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