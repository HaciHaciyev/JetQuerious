package com.hadzhy.jetquerious.reactive;

interface Cancellation {
    void cancel();
    boolean isCancelled();
}
