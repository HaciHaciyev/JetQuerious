package io.github.hacihaciyev.ds;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;

public final class JetMPSC<T> {

  @SuppressWarnings("unused") private final long p00, p01, p02, p03, p04, p05, p06;
  private long head = 0;

  @SuppressWarnings("unused") private final long p10, p11, p12, p13, p14, p15, p16;
  private static final int SEGMENTS = 8;
  private static final int SEGMENT_MASK = SEGMENTS - 1;
  private AtomicLongArray tail = new AtomicLongArray(SEGMENTS);

  @SuppressWarnings("unused") private final long p20, p21, p22, p23, p24, p25, p26;
  private volatile long globalTail = 0;

  @SuppressWarnings("unused") private final long p30, p31, p32, p33, p34, p35, p36;
  private final Cell<T>[] buffer;

  @SuppressWarnings("unused") private final long p37, p38, p39, p40, p41, p42, p43;
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

  private static final VarHandle VH_SEQ;
  static {
    try {
      VH_SEQ = MethodHandles.lookup().findVarHandle(Cell.class, "seq", long.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public JetMPSC(int capacityPowerOfTwo) {
    if (Integer.bitCount(capacityPowerOfTwo) != 1)
      throw new IllegalArgumentException("Capacity must be a power of 2");
    if ((capacityPowerOfTwo & (SEGMENTS - 1)) != 0)
      throw new IllegalArgumentException("capacity must be multiple of SEGMENTS");

    this.mask = capacityPowerOfTwo - 1;
    this.buffer = new Cell[capacityPowerOfTwo];
    for (int i = 0; i < capacityPowerOfTwo; i++) buffer[i] = new Cell<>(i);
    for (int s = 0; s < SEGMENTS; s++) tail.set(s, s);

    p00 = p01 = p02 = p03 = p04 = p05 = p06 = 0L;
    p10 = p11 = p12 = p13 = p14 = p15 = p16 = 0L;
    p20 = p21 = p22 = p23 = p24 = p25 = p26 = 0L;
    p30 = p31 = p32 = p33 = p34 = p35 = p36 = 0L;
    p37 = p38 = p39 = p40 = p41 = p42 = p43 = 0L;
  }

  public boolean offer(T value) {
    int failCount = 0;
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    int segmentIndex = Thread.currentThread().hashCode() & SEGMENT_MASK;

    while (true) {
      long localIndex = tail.getAndAdd(segmentIndex, SEGMENTS);
      Cell<T> cell = buffer[bufferIndex(localIndex)];

      long seq = (long) VH_SEQ.getAcquire(cell);
      long dif = seq - localIndex;

      boolean slotIsReadyForWrite = dif == 0;

      if (slotIsReadyForWrite) {
        long updated = localIndex + 1;

        cell.value = value;
        VH_SEQ.setRelease(cell, updated);
        return true;
      }

      tail.addAndGet(segmentIndex, -SEGMENTS);

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
    synchronizeGlobalTail();
    return globalTail - head;
  }

  public boolean isEmpty() {
    synchronizeGlobalTail();
    return head == globalTail;
  }

  private int bufferIndex(long value) {
    return (int) (value & mask);
  }

  private void synchronizeGlobalTail() {
    long maxNext = Long.MIN_VALUE;
    for (int i = 0; i < SEGMENTS; i++) {
      long t = tail.get(i);
      if (t > maxNext) maxNext = t;
    }
    long lastIssued = maxNext - SEGMENTS;
    if (lastIssued < 0) lastIssued = 0;
    globalTail = lastIssued;
  }
}
