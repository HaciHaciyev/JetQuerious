package io.github.hacihaciyev.ds;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public final class JetMPSC<T> {

  @SuppressWarnings("unused")
  private final long p1, p2, p3, p4, p5, p6, p7;
  private long head = 0;

  @SuppressWarnings("unused")
  private final long p8, p9, p10, p11, p12, p13, p14;
  private volatile long tail = 0;

  @SuppressWarnings("unused")
  private final long p15, p16, p17, p18, p19, p20, p21;
  private final Cell<T>[] buffer;

  @SuppressWarnings("unused") private final long p22, p23, p24, p25, p26, p27, p28;
  private final int mask;

  private static final class Cell<E> {
    volatile long seq;
    E value;

    @SuppressWarnings("unused")
    final long p1, p2, p3, p4, p5, p6;

    Cell(long s) {
      seq = s;
      p1 = p2 = p3 = p4 = p5 = p6 = 0L;
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

  @SuppressWarnings("unchecked")
  public JetMPSC(int capacityPowerOfTwo) {
    if (Integer.bitCount(capacityPowerOfTwo) != 1)
      throw new IllegalArgumentException("Capacity must be a power of 2");

    this.mask = capacityPowerOfTwo - 1;
    this.buffer = new Cell[capacityPowerOfTwo];

    for (int i = 0; i < capacityPowerOfTwo; i++) {
      buffer[i] = new Cell<>(i);
    }

    p1 = p2 = p3 = p4 = p5 = p6 = p7 = 0L;
    p8 = p9 = p10 = p11 = p12 = p13 = p14 = 0L;
    p15 = p16 = p17 = p18 = p19 = p20 = p21 = 0L;
    p22 = p23 = p24 = p25 = p26 = p27 = p28 = 0L;
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
          return true;
        }
      }

      boolean queueIsOvercrowded = dif < 0;
      if (queueIsOvercrowded)
        return false;

      failCount++;
      if (failCount < 12)
        Thread.onSpinWait();

      else if (failCount < 24)
        Thread.yield();

      else {
        long base = 1L << Math.min(failCount - 24, 16);
        long nanos = base + rnd.nextLong(base);
        LockSupport.parkNanos(nanos);
      }
    }
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
