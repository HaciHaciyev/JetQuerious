package com.hadzhy.jetquerious.asynch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import com.hadzhy.jetquerious.ds.JetMPSC;

public final class JetQExecutor {
  private final ExecutorService executor;
  private final JetMPSC<TaskWrapper<?>> queue;
  private volatile boolean shutdown = false;

  private static final int DEFAULT_QUEUE_SIZE_POWER = 16;
  private static final int DEFAULT_WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  public JetQExecutor() {
    this(DEFAULT_QUEUE_SIZE_POWER, DEFAULT_WORKER_THREADS);
  }

  public JetQExecutor(int queueSizePowerOfTwo, int workerThreads) {
    validateParameters(queueSizePowerOfTwo, workerThreads);

    this.queue = new JetMPSC<>(queueSizePowerOfTwo);
    this.executor = Executors.newFixedThreadPool(workerThreads);

    startConsumer();
  }

  /**
   * Submit task for asynchronous execution
   * Hot path - optimized for minimal latency
   */
  public <T> CompletableFuture<T> execute(Supplier<T> task) {
    if (shutdown)
      return CompletableFuture.failedFuture(
          new IllegalStateException("Executor is shutdown"));

    CompletableFuture<T> future = new CompletableFuture<>();
    TaskWrapper<T> wrapper = new TaskWrapper<>(task, future);

    if (queue.offer(wrapper))
      return future;

    else {
      try {
        wrapper.execute();
      } catch (Exception e) {
        future.completeExceptionally(e);
      }

      return future;
    }
  }

  public void shutdown() {
    shutdown = true;
    executor.shutdown();
  }

  private void validateParameters(int queueSizePowerOfTwo, int workerThreads) {
    if (queueSizePowerOfTwo < 0)
      throw new IllegalArgumentException("Queue size power cannot be negative");
    if (queueSizePowerOfTwo > 32)
      throw new IllegalArgumentException("Queue size power too large (max 32)");
    if (workerThreads < 1)
      throw new IllegalArgumentException("Worker threads must be positive");
  }

  private void startConsumer() {
    Thread.startVirtualThread(() -> {
      while (!shutdown || !queue.isEmpty()) {
        TaskWrapper<?> task = queue.poll();

        if (task != null)
          executor.execute(() -> task.execute());
        else
          LockSupport.parkNanos(1_000);
      }
    });
  }

  private static final class TaskWrapper<T> {
    private final Supplier<T> task;
    private final CompletableFuture<T> future;

    TaskWrapper(Supplier<T> task, CompletableFuture<T> future) {
      this.task = task;
      this.future = future;
    }

    void execute() {
      try {
        T result = task.get();
        future.complete(result);
      } catch (Throwable ex) {
        future.completeExceptionally(ex);
      }
    }
  }
}
