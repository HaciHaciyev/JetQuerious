package io.github.hacihaciyev.sql;

import static java.util.Objects.requireNonNull;

import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.jdbc.Transactions;

public record Transaction(JQ[] operations, Savepoint[] savepoints, IsolationLevel isolationLevel, JetQuerious executor)
    implements Transactions {
    
    public Transaction {
        operations = requireNonNull(operations, "Transaction operations cannot be null").clone();
        if (operations.length == 0) throw new IllegalArgumentException("Transaction must have at least one operation");
        savepoints = requireNonNull(savepoints, "Transaction savepoints cannot be null").clone();
        
        for (var op : operations) requireNonNull(op, "Transaction operation cannot be null");
        for (var sp : savepoints) requireNonNull(sp, "Transaction savepoint cannot be null");
        
        requireNonNull(isolationLevel, "Transaction isolation level is required");
        requireNonNull(executor, "Executor cannot be null");
    }
    
    public Transaction(JQ[] operations, Savepoint[] savepoints, IsolationLevel isolationLevel) {
        this(operations, savepoints, isolationLevel, JetQuerious.defaultInstance());
    }
    
    public Transaction(JQ[] operations, Savepoint[] savepoints) {
        this(operations, savepoints, IsolationLevel.DEFAULT, JetQuerious.defaultInstance());
    }
    
    public record Savepoint(int position, String name) {
        public Savepoint {
            requireNonNull(name, "Savepoint name cannot be null");
            if (position < 0) throw new IllegalArgumentException("Savepoint position cannot be negative");
            if (name.trim().isBlank()) throw new IllegalArgumentException("Savepoint name cannot be blank");
            name = name.trim();
        }
    }
    
    public enum IsolationLevel {
        DEFAULT,
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }
}