package io.github.hacihaciyev.asynch;

import io.github.hacihaciyev.util.Sign;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public final class JetDispatcher {
  private static final long IDLE_NANOS = 1_000L;
  private static final int DEFAULT_QUEUE_CAPACITY = 1 << 18;
  private static final Logger log = Logger.getLogger(JetDispatcher.class.getName());

  private final JetMPSC<TaskWrapper<?>> queue;
  private volatile boolean shutdown = false;
  private volatile boolean shutdownGracefully = false;

  public JetDispatcher() {
    this(DEFAULT_QUEUE_CAPACITY);
  }

  public JetDispatcher(int queueCapacity) {
    this.queue = new JetMPSC<>(queueCapacity);
    startConsumer();
  }

  public <T> Sign<T, Exception> submit(Supplier<T> task) {
    requireNonNull(task, "task cannot be null");

    if (shutdown || shutdownGracefully) {
      Sign<T, Exception> failed = new Sign<>();
      failed.err(new IllegalStateException("Dispatcher is shutdown"));
      return failed;
    }

    Sign<T, Exception> sign = new Sign<>();
    TaskWrapper<T> wrapper = new TaskWrapper<>(task, sign);

    if (queue.offer(wrapper)) return sign;

    try {
      executeInlineOrSpawn(wrapper);
    } catch (Throwable ex) {
      sign.err((Exception) (ex instanceof Exception ? ex : new RuntimeException(ex)));
    }

    return sign;
  }

  public void shutdown() { shutdown = true; }

  public Sign<Void, Exception> shutdownGracefully() {
    return shutdownGracefully(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  public Sign<Void, Exception> shutdownGracefully(long timeout, TimeUnit unit) {
    if (timeout < 0) throw new IllegalArgumentException("Timeout cannot be negative");
    if (unit == null) throw new IllegalArgumentException("unit cannot be null");

    shutdownGracefully = true;
    Sign<Void, Exception> sign = new Sign<>();

    Thread.startVirtualThread(() -> {
      long timeoutNanos = unit.toNanos(timeout);
      long start = System.nanoTime();

      while (System.nanoTime() - start < timeoutNanos && !queue.isEmpty()) {
        LockSupport.parkNanos(IDLE_NANOS);
      }

      if (!queue.isEmpty()) {
        log.warning(() -> "Graceful shutdown timeout exceeded. Remaining tasks: " + queue.size());
      } else {
        log.fine("Graceful shutdown completed. All tasks executed.");
      }

      shutdown = true;
      sign.ok(null);
    });

    return sign;
  }

  private void startConsumer() {
    Thread.startVirtualThread(() -> {
      while (!shutdown || !queue.isEmpty()) {

        TaskWrapper<?> task = queue.poll();
        if (task == null) {
          LockSupport.parkNanos(IDLE_NANOS);
          continue;
        }

        try {
          if (Thread.currentThread().isVirtual()) {
            task.execute();
          } else {
            Thread.startVirtualThread(task::execute);
          }
        } catch (Throwable t) {
          log.log(Level.SEVERE, "Failed to dispatch task", t);
        }
      }
    });
  }

  private <T> void executeInlineOrSpawn(TaskWrapper<T> wrapper) {
    if (Thread.currentThread().isVirtual()) {
      wrapper.execute();
      return;
    }

    Thread.startVirtualThread(wrapper::execute);
  }

  private record TaskWrapper<T>(Supplier<T> task, Sign<T, Exception> sign) {
    void execute() {
      try {
        T result = task.get();
        sign.ok(result);
      } catch (Exception ex) {
        sign.err(ex);
      }
    }
  }
}
