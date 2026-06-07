package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.Transaction;

public interface Transactions {

    BoundTransaction transaction(Transaction transaction);
}