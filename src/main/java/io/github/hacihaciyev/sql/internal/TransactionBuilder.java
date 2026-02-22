package io.github.hacihaciyev.sql.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.sql.Transaction.*;

public final class TransactionBuilder {
    private final List<JQ> operations = new ArrayList<>();
    private final List<Savepoint> savepoints = new ArrayList<>();
    private IsolationLevel isolationLevel = IsolationLevel.DEFAULT;
    
    private TransactionBuilder() {}
        
    public TransactionBuilder add(JQ operation) {
        operations.add(requireNonNull(operation, "Transaction operation cannot be null"));
        return this;
    }
        
    public TransactionBuilder savepoint(String name) {
        savepoints.add(new Savepoint(operations.size(), name));
        return this;
    }
        
    public TransactionBuilder isolationLevel(IsolationLevel level) {
        this.isolationLevel = requireNonNull(level, "Transaction isolation level is required");
        return this;
    }
        
    public Transaction build() {
        return new Transaction(operations.toArray(new JQ[0]), savepoints.toArray(new Savepoint[0]), isolationLevel);
    }
}