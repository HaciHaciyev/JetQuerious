package io.github.hacihaciyev.asynch;

@FunctionalInterface
public interface BatchErrorHandler {
  void onErrors(Throwable[] errors);
}
