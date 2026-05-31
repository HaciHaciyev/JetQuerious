package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.Transaction;
import io.github.hacihaciyev.util.Result;

public interface Transactions {

    Result<int[], Exception> transaction(Transaction transaction, Object... args);
}