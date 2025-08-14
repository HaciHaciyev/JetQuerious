package io.github.hacihaciyev.ds;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class JetMPSCStressTest {

    @Test
    void highLoadStressTest() throws InterruptedException {
        final int DURATION_SEC = 5;
        final int PRODUCERS = 4;
        final int BUFFER_SIZE = 256;
        JetMPSC<Long> queue = new JetMPSC<>(BUFFER_SIZE);

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong produced = new AtomicLong();
        AtomicLong consumed = new AtomicLong();

        Thread consumer = new Thread(() -> {
            while (running.get() || !queue.isEmpty()) {
                Long item = queue.poll();
                if (item != null) {
                    consumed.incrementAndGet();
                }
            }
        });
        consumer.start();

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCERS);
        for (int i = 0; i < PRODUCERS; i++) {
            producers.execute(() -> {
                while (running.get()) {
                    if (queue.offer(System.nanoTime())) {
                        produced.incrementAndGet();
                    } else {
                        Thread.onSpinWait();
                    }
                }
            });
        }

        Thread.sleep(DURATION_SEC * 1000);
        running.set(false);
        producers.shutdown();
        producers.awaitTermination(1, TimeUnit.SECONDS);
        consumer.join();

        System.out.printf("[Stress] Produced: %,d items%n", produced.get());
        System.out.printf("[Stress] Consumed: %,d items%n", consumed.get());
        System.out.printf("[Stress] Queue size: %d%n", queue.size());

        assertEquals(produced.get(), consumed.get() + queue.size(),
                "Lost items detected. Produced: " + produced.get() +
                        ", Consumed: " + consumed.get() +
                        ", Remaining: " + queue.size());
    }

    @Test
    @Disabled("Will destroy most average devices on execution")
    void highLoadIoBoundScenario() throws InterruptedException {
        final int PRODUCERS = 10_000;
        final int TOTAL_ITEMS = 100_000;
        final int QUEUE_CAPACITY = 1 << 16;
        final int CONSUMER_DELAY_MS = 5;

        JetMPSC<Task> queue = new JetMPSC<>(QUEUE_CAPACITY);
        AtomicInteger consumed = new AtomicInteger();

        Thread consumer = new Thread(() -> {
            Task[] batch = new Task[64];
            while (consumed.get() < TOTAL_ITEMS) {
                int count = queue.pollBatch(batch, batch.length);
                if (count > 0) {
                    try {
                        Thread.sleep(CONSUMER_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    consumed.addAndGet(count);
                }
            }
        });
        consumer.start();

        ExecutorService producerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(PRODUCERS);

        for (int i = 0; i < PRODUCERS; i++) {
            producerExecutor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        Task task = new Task();
                        while (!queue.offer(task)) {
                            Thread.onSpinWait();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Producers didn't finish in time");
        consumer.join();
        producerExecutor.shutdown();

        assertEquals(TOTAL_ITEMS, consumed.get(), "Items lost");
        assertTrue(queue.isEmpty(), "Queue should be empty");
    }

    static class Task {}
}
