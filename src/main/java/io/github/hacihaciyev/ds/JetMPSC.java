package io.github.hacihaciyev.ds;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public final class JetMPSC<T> {

  @SuppressWarnings("unused") private final long p00, p01, p02, p03, p04, p05, p06;
  private long head = 0;

  @SuppressWarnings("unused") private final long p10, p11, p12, p13, p14, p15, p16;
  private volatile long tail = 0;

  @SuppressWarnings("unused") private final long p20, p21, p22, p23, p24, p25, p26;
  private final Cell<T>[] buffer;

  @SuppressWarnings("unused") private final long p30, p31, p32, p33, p34, p35, p36;
  private final int mask;

  private static final class Cell<E> {
    @SuppressWarnings("unused") private final long p00, p01, p02, p03, p04, p05, p06;

    volatile long seq;
    E value;

    @SuppressWarnings("unused") private final long p10, p11, p12, p13, p14, p15, p16;
    Cell(long s) {
      seq = s;
      p00 = p01 = p02 = p03 = p04 = p05 = p06 = 0L;
      p10 = p11 = p12 = p13 = p14 = p15 = p16 = 0L;
    }
  }

  private static final VarHandle VH_TAIL;
  private static final VarHandle VH_SEQ;
  static {
    try {
      VH_TAIL = MethodHandles.lookup().findVarHandle(JetMPSC.class, "tail", long.class);
      VH_SEQ = MethodHandles.lookup().findVarHandle(Cell.class, "seq", long.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private volatile long metrics = 0L; // high 32 bits = success, low 32 bits = failed
  private volatile int adaptiveParams = (12 << 16) | 24; // spinLimit(16) | yieldLimit(16)
  private volatile long lastAdaptTime = 0L;

  private static long incrementSuccess(long current) {
    return current + (1L << 32);
  }

  private static long incrementFailed(long current) {
    return current + 1L;
  }

  private static int getSuccess(long metrics) {
    return (int) (metrics >>> 32);
  }

  private static int getFailed(long metrics) {
    return (int) (metrics & 0xFFFFFFFFL);
  }

  @SuppressWarnings("unchecked")
  public JetMPSC(int capacityPowerOfTwo) {
    if (Integer.bitCount(capacityPowerOfTwo) != 1)
      throw new IllegalArgumentException("Capacity must be a power of 2");

    this.mask = capacityPowerOfTwo - 1;
    this.buffer = new Cell[capacityPowerOfTwo];
    for (int i = 0; i < capacityPowerOfTwo; i++) buffer[i] = new Cell<>(i);

    p00 = p01 = p02 = p03 = p04 = p05 = p06 = 0L;
    p10 = p11 = p12 = p13 = p14 = p15 = p16 = 0L;
    p20 = p21 = p22 = p23 = p24 = p25 = p26 = 0L;
    p30 = p31 = p32 = p33 = p34 = p35 = p36 = 0L;
  }

  public boolean offer(T value) {
    int failCount = 0;
    ThreadLocalRandom rnd = ThreadLocalRandom.current();

    while (true) {
      long t = tail;
      Cell<T> cell = buffer[bufferIndex(t)];

      long seq = (long) VH_SEQ.getAcquire(cell);
      long dif = seq - t;

      boolean slotIsReadyForWrite = dif == 0;

      if (slotIsReadyForWrite) {
        long expected = t;
        long updated = t + 1;

        if (VH_TAIL.compareAndSet(this, expected, updated)) {
          cell.value = value;
          VH_SEQ.setRelease(cell, updated);
          long currentMetrics = metrics;
          metrics = incrementSuccess(currentMetrics);
          return true;
        }
      }

      boolean queueIsOvercrowded = dif < 0;
      if (queueIsOvercrowded)
        return false;

      if (failCount == 0) {
        long currentMetrics = metrics;
        metrics = incrementFailed(currentMetrics);
      }

      int params = adaptiveParams;
      int spinLimit = params >>> 16;
      int yieldLimit = params & 0xFFFF;

      failCount++;

      if (failCount < spinLimit)
        Thread.onSpinWait();

      else if (failCount < spinLimit + yieldLimit)
        Thread.yield();

      else {
        long base = 1L << Math.min(failCount - 24, 16);
        long nanos = base + rnd.nextLong(base);
        LockSupport.parkNanos(nanos);
      }

      if (failCount == 32) tryAdapt();
    }
  }

  private void tryAdapt() {
    long now = System.nanoTime();
    long lastTime = lastAdaptTime;

    boolean wasAdaptedNotLongAgo = now - lastTime < 100_000_000L;
    if (wasAdaptedNotLongAgo) return;

    boolean toMuchAdaptations = lastTime != 0L && now - lastTime < 200_000_000L;
    if (toMuchAdaptations) return;

    lastAdaptTime = now;

    long currentMetrics = metrics;
    int success = getSuccess(currentMetrics);
    int failed = getFailed(currentMetrics);

    boolean notEnoughStats = success + failed < 100;
    if (notEnoughStats) return;

    metrics = 0L;
    double failureRate = (double) failed / (success + failed);
    int currentParams = adaptiveParams;
    int spinLimit = currentParams >>> 16;
    int yieldLimit = currentParams & 0xFFFF;

    boolean concurrencyIsToHighIncreaseSpinning = failureRate > 0.6;
    if (concurrencyIsToHighIncreaseSpinning) {
      spinLimit = Math.max(4, spinLimit - 2);
      yieldLimit = Math.max(8, yieldLimit - 4);
    }

    boolean concurrencyIsLowDecreaseSpinning = failureRate < 0.3;
    if (concurrencyIsLowDecreaseSpinning) {
      spinLimit = Math.min(32, spinLimit + 2);
      yieldLimit = Math.min(48, yieldLimit + 4);
    }

    adaptiveParams = (spinLimit << 16) | yieldLimit;
  }

  public T poll() {
    long h = head;
    Cell<T> cell = buffer[bufferIndex(h)];

    long seq = (long) VH_SEQ.getAcquire(cell);
    long dif = seq - (h + 1);

    boolean slotIsReadyForRead = dif == 0;
    if (slotIsReadyForRead) {
      head = h + 1;

      T v = cell.value;
      cell.value = null;
      VH_SEQ.setRelease(cell, h + mask + 1);
      return v;
    }

    return null;
  }

  /**
   * Poll up to 'max' elements into the given buffer.
   * Returns the number of elements actually polled.
   */
  public int pollBatch(T[] dst, int max) {
    int count = 0;
    long h = head;

    while (count < max) {
      Cell<T> cell = buffer[bufferIndex(h)];
      long seq = (long) VH_SEQ.getAcquire(cell);
      long dif = seq - (h + 1);

      if (dif == 0) {
        h++;
        T v = cell.value;
        cell.value = null;
        VH_SEQ.setRelease(cell, h + mask + 1);
        dst[count++] = v;
      } else
        break;
    }

    if (count > 0)
      head = h;

    return count;
  }

  public long size() {
    return tail - head;
  }

  public boolean isEmpty() {
    return head == tail;
  }

  private int bufferIndex(long value) {
    return (int) (value & mask);
  }
}
