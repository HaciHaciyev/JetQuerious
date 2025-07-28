package com.hadzhy.jetquerious.reactive;

interface Callback<T> {
    void onSuccess(T item);

    void onFailure(Throwable failure);
}
