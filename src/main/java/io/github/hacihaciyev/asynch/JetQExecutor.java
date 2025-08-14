package io.github.hacihaciyev.asynch;

import io.github.hacihaciyev.ds.JetMPSC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JetQExecutor {
  private final int batchSize;
  private final int highWatermark;
  private final int lowWatermark;
  private final JetMPSC<TaskWrapper<?>> queue;
  private final BatchErrorHandler batchErrorHandler;
  private volatile boolean shutdown = false;

  private static final int DEFAULT_BATCH_SIZE = 64;
  private static final int DEFAULT_QUEUE_CAPACITY = 65536; // 2^16
  private static final Logger log = Logger.getLogger(JetQExecutor.class.getName());

  public JetQExecutor() {
    this(DEFAULT_QUEUE_CAPACITY, DEFAULT_BATCH_SIZE, null);
  }

  public JetQExecutor(int queueCapacity, int batchSize, BatchErrorHandler errorHandler) {
    if (queueCapacity <= 0)
      throw new IllegalArgumentException("Queue capacity must be positive");

    if (queueCapacity > DEFAULT_QUEUE_CAPACITY)
      log.warning(() -> "Queue capacity " + queueCapacity +
          " is too large. Big queues rarely improve throughput " +
          "and may increase latency and memory usage.");

    if (batchSize <= 0)
      throw new IllegalArgumentException("Batch size must be positive.");

    if (batchSize > DEFAULT_BATCH_SIZE)
      log.warning(() -> "Batch size " + batchSize +
          " is too large. Be carefull.");

    this.queue = new JetMPSC<>(queueCapacity);
    this.batchSize = batchSize;
    this.highWatermark = (int) (queueCapacity * 0.8);
    this.lowWatermark = (int) (queueCapacity * 0.3);
    this.batchErrorHandler = errorHandler != null ? errorHandler : errors -> {
      for (Throwable e : errors) {
        log.log(Level.SEVERE, "Task execution failed", e);
      }
    };

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
  }

  /**
   * Graceful shutdown - waits for queue to be empty
   */
  public CompletableFuture<Void> shutdownGracefully(long timeout, TimeUnit unit) {
    if (timeout < 0)
      throw new IllegalArgumentException("Timeout cannot be negative: " + timeout);
    if (unit == null)
      throw new IllegalArgumentException("TimeUnit cannot be null");

    shutdown = true;

    return CompletableFuture.runAsync(() -> {
      long timeoutNanos = unit.toNanos(timeout);
      long startTime = System.nanoTime();
      long pollInterval = Math.min(10_000_000L, timeoutNanos / 100);

      while (!queue.isEmpty()) {
        long elapsed = System.nanoTime() - startTime;
        if (elapsed >= timeoutNanos) {
          log.warning("Graceful shutdown timeout exceeded. Queue size: " + queue.size());
          break;
        }

        LockSupport.parkNanos(pollInterval);
      }

      log.fine("JetQExecutor graceful shutdown completed. Queue size: " + queue.size());
    });
  }

  /**
   * Graceful shutdown without timeout
   */
  public CompletableFuture<Void> shutdownGracefully() {
    return shutdownGracefully(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  private void startConsumer() {
    Thread.startVirtualThread(() -> {
      boolean bulkMode = false;
      TaskWrapper<?>[] buf = new TaskWrapper[batchSize];

      while (!shutdown || !queue.isEmpty()) {
        long size = queue.size();
        if (size > highWatermark)
          bulkMode = true;
        else if (size < lowWatermark)
          bulkMode = false;

        if (!bulkMode) {
          executeSingleTask();
          continue;
        }

        int n = queue.pollBatch(buf, batchSize);
        if (n == 0) {
          LockSupport.parkNanos(1_000);
          continue;
        }

        executeBatch(buf, n);
      }
    });
  }

  private void executeSingleTask() {
    TaskWrapper<?> task = queue.poll();

    if (task != null)
      Thread.startVirtualThread(() -> task.execute());
    else
      LockSupport.parkNanos(1_000);
  }

  private void executeBatch(TaskWrapper<?>[] buf, int n) {
    Thread.startVirtualThread(() -> {
      List<Throwable> errors = new ArrayList<>();

      for (int i = 0; i < n; i++) {
        try {
          buf[i].execute();
        } catch (Exception e) {
          errors.add(e);
        }
      }

      if (!errors.isEmpty())
        this.batchErrorHandler.onErrors(errors.toArray(new Throwable[errors.size()]));
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
