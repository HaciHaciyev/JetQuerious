package io.github.hacihaciyev.ds;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public final class JetMPSC<T> {

  @SuppressWarnings("unused") private final long p1, p2, p3, p4, p5, p6, p7;
  private long head = 0;
  @SuppressWarnings("unused") private final long p8, p9, p10, p11, p12, p13, p14;
  private volatile long tail = 0;
  @SuppressWarnings("unused") private final long p15, p16, p17, p18, p19, p20, p21;

  @SuppressWarnings("unused")  private final long p22, p23, p24, p25, p26, p27, p28;
  private final T[] buffer;
  @SuppressWarnings("unused") private final long p29, p30, p31, p32, p33, p34, p35;
  private final long[] seqs;

  private final int mask;

  private static final VarHandle VH_TAIL;
  private static final VarHandle VH_SEQ;
  static {
    try {
      VH_TAIL = MethodHandles.lookup().findVarHandle(JetMPSC.class, "tail", long.class);
      VH_SEQ = MethodHandles.arrayElementVarHandle(long[].class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public JetMPSC(int capacityPowerOfTwo) {
    if (Integer.bitCount(capacityPowerOfTwo) != 1)
      throw new IllegalArgumentException("Capacity must be a power of 2");

    this.mask = capacityPowerOfTwo - 1;
    this.seqs = new long[capacityPowerOfTwo];
    this.buffer = (T[]) new Object[capacityPowerOfTwo];
    for (int i = 0; i < capacityPowerOfTwo; i++) seqs[i] = i;

    p1 = p2 = p3 = p4 = p5 = p6 = p7 = 0L;
    p8 = p9 = p10 = p11 = p12 = p13 = p14 = 0L;
    p15 = p16 = p17 = p18 = p19 = p20 = p21 = 0L;
    p22 = p23 = p24 = p25 = p26 = p27 = p28 = 0L;
    p29 = p30 = p31 = p32 = p33 = p34 = p35 = 0L;
  }

  public boolean offer(T value) {
    int failCount = 0;
    ThreadLocalRandom rnd = ThreadLocalRandom.current();

    while (true) {
      long t = tail;
      int idx = bufferIndex(t);
      long seq = (long) VH_SEQ.getAcquire(seqs, idx);

      long dif = seq - t;
      boolean slotIsReadyForWrite = dif == 0;

      if (slotIsReadyForWrite) {
        long expected = t;
        long updated = t + 1;

        if (VH_TAIL.compareAndSet(this, expected, updated)) {
          buffer[idx] = value;
          VH_SEQ.setRelease(seqs, idx, updated);
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
    int idx = bufferIndex(h);
    long seq = (long) VH_SEQ.getAcquire(seqs, idx);

    long dif = seq - (h + 1);
    boolean slotIsReadyForRead = dif == 0;

    if (slotIsReadyForRead) {
      T v = buffer[idx];
      buffer[idx] = null;
      head = h + 1;
      VH_SEQ.setRelease(seqs, idx, h + mask + 1);
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
      int idx = bufferIndex(h);
      T v = buffer[idx];
      long seq = (long) VH_SEQ.getAcquire(seqs, idx);

      long dif = seq - (h + 1);
      boolean slotIsReadyForRead = dif == 0;

      if (slotIsReadyForRead) {
        h++;
        buffer[idx] = null;
        VH_SEQ.setRelease(seqs, idx, h + mask + 1);
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
