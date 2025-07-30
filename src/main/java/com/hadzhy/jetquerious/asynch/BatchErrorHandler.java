package com.hadzhy.jetquerious.asynch;

@FunctionalInterface
public interface BatchErrorHandler {
  void onErrors(Throwable[] errors);
}
