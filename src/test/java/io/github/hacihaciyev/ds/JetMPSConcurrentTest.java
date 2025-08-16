package io.github.hacihaciyev.ds;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.junit.jupiter.api.RepeatedTest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

class JetMPSConcurrentTest {

    private static final JetMPSC<Integer> QUEUE = new JetMPSC<>(1 << 20);

    @RepeatedTest(10)
    void correctMPSCTest() throws Exception {
        final int PRODUCERS = 1 << 22;
        final int ITEMS_PER_PRODUCER = 10;
        final int TOTAL_ITEMS = PRODUCERS * ITEMS_PER_PRODUCER;

        ExecutorService producerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger produced = new AtomicInteger();

        Thread consumer = Thread.ofVirtual().start(() -> {
            int consumed = 0;
            while (consumed < TOTAL_ITEMS) {
                Integer item = QUEUE.poll();
                if (item != null) {
                    consumed++;
                }
            }
        });

        for (int i = 0; i < PRODUCERS; i++) {
            producerExecutor.execute(() -> {
                for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                    while (!QUEUE.offer(j)) {
                        Thread.onSpinWait();
                    }
                    produced.incrementAndGet();
                }
            });
        }

        producerExecutor.shutdown();
        producerExecutor.awaitTermination(1, TimeUnit.MINUTES);
        consumer.join();

        assertEquals(TOTAL_ITEMS, produced.get());
    }

    @RepeatedTest(10)
    void correctDisruptorPollOfferEquivalent() throws Exception {
        final int PRODUCERS = 1 << 22;
        final int ITEMS_PER_PRODUCER = 10;
        final int TOTAL_ITEMS = PRODUCERS * ITEMS_PER_PRODUCER;

        RingBuffer<ValueEvent> ringBuffer = RingBuffer.createMultiProducer(
                ValueEvent::new,
                1 << 20,
                new YieldingWaitStrategy()
        );

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();

        Thread consumer = Thread.ofVirtual().start(() -> {
            long nextSequence = 0;
            while (nextSequence < TOTAL_ITEMS) {
                long availableSequence = ringBuffer.getCursor();
                while (nextSequence <= availableSequence) {
                    ringBuffer.get(nextSequence);
                    consumed.incrementAndGet();
                    nextSequence++;
                }
                Thread.onSpinWait();
            }
        });

        for (int i = 0; i < PRODUCERS; i++) {
            executor.execute(() -> {
                for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                    long sequence = ringBuffer.next();
                    try {
                        ValueEvent event = ringBuffer.get(sequence);
                        event.value = j;
                    } finally {
                        ringBuffer.publish(sequence);
                        produced.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        consumer.join();

        assertEquals(TOTAL_ITEMS, produced.get());
        assertEquals(TOTAL_ITEMS, consumed.get());
    }

    static class ValueEvent {
        public int value;
    }
}