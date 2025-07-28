package com.hadzhy.jetquerious.reactive;

public interface Subscription {
    void request(long n);
    void cancel();
}
